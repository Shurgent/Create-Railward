"""Editable, reference-based bogey model. Game resource installation is opt-in.

Coordinates are Blockbench pixels: X across track, Y up, Z along train;
wheel axes at Y=0, Z=-32/0/+32. Requires Pillow and numpy for previews.
Run: python tools/generate_bogey_triple_axle_v2.py
Install for all four targets: add --install-resources (shared src/main/resources).
"""

from __future__ import annotations

import argparse
import base64
import json
import math
import shutil
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

import numpy as np
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
NAME = "bogey_triple_axle_equalized_locomotive_v2"
GAME_NAME = "bogey_triple_axle_equalized_locomotive"
OUT = ROOT / "tools/blockbench/bogeys/triple_axle_v2"
ATLAS_SIZE = 128
AXLES = (-32, 0, 32)
# Axle / individual hinge / spring / hydraulic damper. The opposed arms share
# one broad frame apron, but keep separate pins. Springs are next to the box;
# dampers occupy the extended free tips of the arms.
BALANCE_LAYOUT = ((-32, -19.3, -36.8, -39), (0, -12.7, 4.8, 8), (32, 19.3, 36.8, 39))
AXLEBOX_HALF_LENGTH = 3.2
CROSSMEMBER_HALF_DEPTH = 1.5
CROSSMEMBER_BOTTOM = -6.6
MOTOR_HALF_HEIGHT = 6
FRAME_BOTTOM = 4.8
FRAME_TOP = 9.8
FRAME_ATTACHMENT_DROP = 7.5-FRAME_BOTTOM
HINGE_BRACKET_TOP = 9.5-FRAME_ATTACHMENT_DROP
HINGE_BRIDGE_BOTTOM = HINGE_BRACKET_TOP-.7
BALANCE_PIVOT_Y = .4
BALANCE_X_CENTER = 18.2
BALANCE_X_HALF_WIDTH = 1
BRACKET_INNER_X = (16.35,16.85)
BRACKET_OUTER_X = (19.55,20.05)
# Keep the visual brake bracket short while placing its shaft below the
# shortened hydraulic-damper bodies rather than through them.
BRAKE_BRACKET_BOTTOM = -.8
BRAKE_BRACKET_TOP = BRAKE_BRACKET_BOTTOM+8.2
BRAKE_SHAFT_Y = 0
BRAKE_SHAFT_Z = 39.5
TILES = {
    "rail": (1, 1, 65, 17), "steel": (1, 20, 33, 36),
    "balance": (35, 20, 67, 32), "wheel": (70, 1, 94, 25),
    "tread": (96, 1, 112, 17), "cap": (70, 28, 86, 44),
    "spring": (88, 20, 104, 36), "motor": (1, 39, 33, 55),
    "brake": (35, 39, 51, 55), "rubber": (53, 39, 69, 55),
    "rod": (106, 20, 122, 36), "grease": (70, 48, 86, 64),
    "bolted": (88, 39, 104, 55),
}
PALETTE = {
    "rail": (48, 49, 48), "steel": (55, 56, 55),
    "balance": (51, 52, 51), "wheel": (45, 46, 45),
    "tread": (162, 163, 162), "cap": (60, 61, 60),
    "spring": (40, 41, 40), "motor": (43, 44, 43),
    "brake": (51, 52, 51), "rubber": (28, 29, 28),
    "rod": (111, 112, 111), "grease": (36, 37, 36),
    "bolted": (55, 56, 55),
}


def vec(v):
    return np.asarray(v, dtype=float)


def mount_y(previous_y):
    """Lower upper-frame attachments; wheel and balance-arm datums stay fixed."""
    return previous_y-FRAME_ATTACHMENT_DROP


def unit(v):
    v = vec(v)
    return v / max(np.linalg.norm(v), 1e-12)


def uid(name):
    return str(uuid5(NAMESPACE_URL, "railward:triple_axle_v2/" + name))


@dataclass
class Face:
    vertices: list
    uv: list
    material: str


@dataclass
class Part:
    name: str
    group: str
    faces: list[Face] = field(default_factory=list)


PARTS: list[Part] = []
BRAKE_MOUNTS: list[dict] = []
UV_PATCHES: list[dict] = []


def material_density(material):
    # Metal surfaces use the Minecraft 16px/block scale. Coil and small bearing
    # markings retain a little extra resolution, but square texels in both axes.
    return 2 if material in ("spring","cap","bolted") else 1


def remap_uvs():
    """Unwrap each planar surface without scaling U and V independently.

    Coplanar cap triangles share one island. Reusable material patches are
    packed with gutters; long faces get long strips, never stretched squares.
    """
    global ATLAS_SIZE
    islands=[]
    patches={}
    for p in PARTS:
        planes=defaultdict(list)
        for f in p.faces:
            points=np.asarray(f.vertices)
            normal=unit(np.cross(points[1]-points[0],points[2]-points[0]))
            key=(f.material,tuple(np.round(normal,6)),round(float(normal@points[0]),6))
            planes[key].append(f)
        for (material,normal_key,_),faces in planes.items():
            normal=unit(normal_key)
            # Prefer vertical grain on side walls, horizontal grain on top faces.
            if abs(normal[1])<.95:
                down=vec((0,-1,0))
                v=unit(down-normal*(down@normal))
                u=unit(np.cross(v,normal))
            else:
                across=vec((1,0,0))
                u=unit(across-normal*(across@normal))
                v=unit(np.cross(normal,u))
            density=material_density(material)
            coords=[np.stack((np.asarray(f.vertices)@u,np.asarray(f.vertices)@v),axis=1)*density for f in faces]
            allcoords=np.concatenate(coords)
            # Stable integer origins also preserve the texture grid across cap triangles.
            lo=np.floor(allcoords.min(axis=0)+1e-6)
            hi=np.ceil(allcoords.max(axis=0)-1e-6)
            w,h=np.maximum(1,hi-lo).astype(int)
            patch_key=(material,int(w),int(h))
            patches.setdefault(patch_key,{"material":material,"width":int(w),"height":int(h)})
            islands.append((patch_key,faces,[c-lo for c in coords]))
    ordered=sorted(patches.items(),key=lambda entry:(-entry[1]["height"],-entry[1]["width"],entry[0][0]))
    # First-fit shelves. This packs the material islands, not a bitmap per face.
    for size in (128,256,512):
        shelves=[]
        positions={}
        used_height=0
        for key,p in ordered:
            pw,ph=p["width"]+2,p["height"]+2
            if pw>size or ph>size: break
            shelf=next((r for r in shelves if r["height"]>=ph and r["x"]+pw<=size),None)
            if shelf is None:
                if used_height+ph>size: break
                shelf={"y":used_height,"height":ph,"x":0}
                shelves.append(shelf)
                used_height+=ph
            positions[key]=(shelf["x"]+1,shelf["y"]+1)
            shelf["x"]+=pw
        else:
            ATLAS_SIZE=size
            break
    else:
        raise RuntimeError("UV material patches do not fit in a 512px atlas")
    UV_PATCHES.clear()
    for key,p in ordered:
        x,y=positions[key]
        UV_PATCHES.append({**p,"x":x,"y":y})
    for key,faces,coords in islands:
        origin=np.asarray(positions[key])
        for f,c in zip(faces,coords):
            f.uv=(c+origin).tolist()
    (OUT/"uv_layout.json").write_text(json.dumps({"atlas_size":ATLAS_SIZE,
                "pixels_per_block":16,"detail_pixels_per_block":32,"patches":UV_PATCHES},indent=2),encoding="utf-8")


def part(name, group):
    result = Part(name, group)
    PARTS.append(result)
    return result


def add_face(p, vertices, material, uv=None, outward=None):
    vertices = [vec(v) for v in vertices]
    # Remove duplicate vertices at revolution pole caps.
    indices = []
    for i, vertex in enumerate(vertices):
        if not any(np.linalg.norm(vertex - vertices[j]) < 1e-8 for j in indices):
            indices.append(i)
    vertices = [vertices[i] for i in indices]
    if len(vertices) < 3:
        return
    if uv is not None:
        uv = [uv[i] for i in indices]
    normal = np.cross(vertices[1] - vertices[0], vertices[2] - vertices[0])
    if outward is not None and np.dot(normal, outward) < 0:
        vertices.reverse()
        if uv is not None:
            uv.reverse()
    if uv is None:
        x0, y0, x1, y1 = TILES[material]
        uv = [(x0, y1), (x1, y1), (x1, y0), (x0, y0)][:len(vertices)]
    p.faces.append(Face([v.tolist() for v in vertices], uv, material))


def prism(name, group, axis, a, b, profile, material="steel"):
    """Extrude a CONVEX 2D profile; side faces stay quads, caps triangles."""
    p = part(name, group)
    # Cyclic bases have consistent handedness for all three extrusion axes.
    idx = {"X": (0, 1, 2), "Y": (1, 2, 0), "Z": (2, 0, 1)}[axis]
    def point(t, xy):
        v = np.zeros(3)
        v[idx[0]], v[idx[1]], v[idx[2]] = t, xy[0], xy[1]
        return v
    rings = [[point(t, xy) for xy in profile] for t in (min(a, b), max(a, b))]
    center = np.mean(rings, axis=(0, 1))
    for i in range(len(profile)):
        j = (i + 1) % len(profile)
        face = [rings[0][i], rings[0][j], rings[1][j], rings[1][i]]
        add_face(p, face, material, outward=np.mean(face, axis=0) - center)
    for k, ring in enumerate(rings):
        outward = np.zeros(3)
        outward[idx[0]] = -1 if k == 0 else 1
        # Planar UVs follow the silhouette, instead of repeating a tile per triangle.
        u0, v0, u1, v1 = TILES[material]
        profile_array = np.asarray(profile)
        lo, hi = profile_array.min(axis=0), profile_array.max(axis=0)
        def tex(i):
            xy = (profile_array[i] - lo) / np.maximum(hi - lo, 1e-9)
            return (u0 + xy[1] * (u1-u0), v1 - xy[0] * (v1-v0))
        for i in range(1, len(ring)-1):
            add_face(p, [ring[0], ring[i], ring[i+1]], material,
                     [tex(0), tex(i), tex(i+1)], outward)
    return p


def box(name, group, bounds, material="steel", bevel=0):
    x0, y0, z0, x1, y1, z1 = bounds
    if bevel:
        r = min(bevel, (y1-y0)/3, (z1-z0)/3)
        profile = [(y0+r,z0),(y1-r,z0),(y1,z0+r),(y1,z1-r),
                   (y1-r,z1),(y0+r,z1),(y0,z1-r),(y0,z0+r)]
    else:
        profile = [(y0,z0),(y1,z0),(y1,z1),(y0,z1)]
    return prism(name, group, "X", x0, x1, profile, material)


def beam(name, group, start, end, width, thickness, material="steel"):
    p = part(name, group)
    start, end = vec(start), vec(end)
    d = unit(end-start)
    helper = vec((1,0,0)) if abs(d[0]) < .9 else vec((0,1,0))
    u = unit(np.cross(d, helper)) * width/2
    v = unit(np.cross(d, u)) * thickness/2
    points = [c + u*su + v*sv for c in (start,end) for su,sv in ((-1,-1),(1,-1),(1,1),(-1,1))]
    center = (start+end)/2
    for ids in ((0,1,2,3),(4,5,6,7),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0)):
        face = [points[i] for i in ids]
        add_face(p, face, material, outward=np.mean(face,axis=0)-center)
    return p


def cylinder(name, group, axis, center, radius, length, material="steel", segments=8, caps=None):
    p = part(name, group)
    center = vec(center)
    idx = {"X": (0,1,2), "Y": (1,2,0), "Z": (2,0,1)}[axis]
    rings = []
    for sign in (-1, 1):
        ring = []
        for i in range(segments):
            a = i * math.tau / segments
            v = center.copy()
            v[idx[0]] += sign * length / 2
            v[idx[1]] += math.cos(a) * radius
            v[idx[2]] += math.sin(a) * radius
            ring.append(v)
        rings.append(ring)
    for i in range(segments):
        j = (i+1)%segments
        vertices = [rings[0][i],rings[0][j],rings[1][j],rings[1][i]]
        x0,y0,x1,y1 = TILES[material]
        # The long direction of a spring is vertical in its UV map.
        uv = [(x0,y1),(x1,y1),(x1,y0),(x0,y0)]
        add_face(p, vertices, material, uv, np.mean(vertices,axis=0)-center)
    for k,ring in enumerate(rings):
        cap_mat = caps or material
        x0,y0,x1,y1 = TILES[cap_mat]
        cap_center = center.copy()
        cap_center[idx[0]] += (-1 if k==0 else 1)*length/2
        outward = np.zeros(3)
        outward[idx[0]] = -1 if k==0 else 1
        def tex(v):
            return ((x0+x1)/2+(v[idx[1]]-center[idx[1]])*(x1-x0)/(2*radius),
                    (y0+y1)/2-(v[idx[2]]-center[idx[2]])*(y1-y0)/(2*radius))
        for i in range(segments):
            vertices = [cap_center,ring[i],ring[(i+1)%segments]]
            add_face(p,vertices,cap_mat,[tex(v) for v in vertices],outward)
    return p


def make_wheelset(z, label):
    group = "wheelset_" + label
    box("square_axle",group,(-18.6,-.8,z-.8,18.6,.8,z+.8),"grease")
    for s in (-1,1):
        p = part("octagonal_wheel_"+str(s),group)
        # Dish, flange, tread and hub are one joined revolved surface, not stacked discs.
        profile = [(11.5,0),(11.5,2),(11.8,6.8),(11.8,8.6),(12.6,8.6),
                   (12.6,8),(15.05,8),(15.35,7.7),(15.35,6.65),
                   (14.35,5.65),(14.05,2.6),(15.65,2),(15.65,0)]
        for j,((x0,r0),(x1,r1)) in enumerate(zip(profile,profile[1:])):
            mat = "tread" if j in (3,4,5,6,7) else "wheel"
            for i in range(8):
                a,b = i*math.tau/8,(i+1)*math.tau/8
                vertices = [(s*x0,r0*math.cos(a),z+r0*math.sin(a)),
                            (s*x0,r0*math.cos(b),z+r0*math.sin(b)),
                            (s*x1,r1*math.cos(b),z+r1*math.sin(b)),
                            (s*x1,r1*math.cos(a),z+r1*math.sin(a))]
                if mat == "wheel":
                    u0,v0,u1,v1 = TILES[mat]
                    uv = [((u0+u1)/2+v[2]-z,(v0+v1)/2-v[1]) for v in vertices]
                else:
                    u0,v0,u1,v1 = TILES[mat]
                    uv = [(u0+i*2,v0),(u0+(i+1)*2,v0),
                          (u0+(i+1)*2,v1),(u0+i*2,v1)]
                if s<0:
                    vertices.reverse()
                    uv.reverse()
                add_face(p,vertices,mat,uv)


def build_geometry():
    for z,label in zip(AXLES,("front","center","rear")):
        make_wheelset(z,label)
    for s in (-1,1):
        sx = s*17.5
        g = "frame/side_" + ("left" if s<0 else "right")
        box("longitudinal_beam",g,(sx-1.5,FRAME_BOTTOM,-39,sx+1.5,FRAME_TOP,39),"rail",.7)
        box("top_flange",g,(sx-1.7,FRAME_TOP-.3,-39,sx+1.7,FRAME_TOP+.3,39),"steel")
        # The front and middle arms attach to ONE broad apron, with two distinct
        # pins. The third arm uses the narrower rear apron.
        shared_profile=[(-2.9,-20.9),(-2.9,-11.1),(-2.1,-10.4),
                        (HINGE_BRACKET_TOP,-10.4),(HINGE_BRACKET_TOP,-21.6),(-2.1,-21.6)]
        pivot = BALANCE_LAYOUT[2][1]
        rear_profile=[(-2.9,pivot-1.6),(-2.9,pivot+1.6),(-2.1,pivot+2.3),
                      (HINGE_BRACKET_TOP,pivot+2.3),(HINGE_BRACKET_TOP,pivot-2.3),(-2.1,pivot-2.3)]
        # Two wider cheeks form one fork around the arm. The arm occupies the
        # empty slot, not the solid body of a single plate. A top web joins them.
        for name,profile in (("shared_balance_hinge_bracket",shared_profile),
                             ("rear_balance_hinge_bracket",rear_profile)):
            prism(name,g,"X",s*BRACKET_OUTER_X[0],s*BRACKET_OUTER_X[1],profile,"rail")
            prism(name+"_inner_cheek",g,"X",s*BRACKET_INNER_X[0],s*BRACKET_INNER_X[1],profile,"rail")
            z0,z1=min(p[1] for p in profile),max(p[1] for p in profile)
            x0,x1=sorted((s*BRACKET_INNER_X[0],s*BRACKET_OUTER_X[1]))
            box(name+"_bridge",g,(x0,HINGE_BRIDGE_BOTTOM,z0,x1,HINGE_BRACKET_TOP,z1),"rail")
        for index,(_,pivot,_,_) in enumerate(BALANCE_LAYOUT):
            cylinder("pivot_pin_"+str(index+1),g,"X",(s*BALANCE_X_CENTER,BALANCE_PIVOT_Y,pivot),
                     1.15,4.3,"cap",caps="cap")
        # Independent axlebox balance arms; pivot -> box -> sprung free end.
        for index,(z,pivot,spring,damper_z) in enumerate(BALANCE_LAYOUT):
            eg = f"frame/equalizers/{'left' if s<0 else 'right'}_{index+1}"
            lo,hi = sorted((pivot,damper_z))
            if pivot<damper_z:
                arm_profile=[(-2.4,lo+.4),(-2.4,hi-.8),(-.8,hi+.2),(1,hi+.2),
                             (3.2,z+1.5),(3.2,lo+.4),(2.7,lo-.4),(-1.9,lo-.4)]
            else:
                arm_profile=[(-2.4,lo+.8),(-2.4,hi-.4),(-1.9,hi+.4),(2.7,hi+.4),
                             (3.2,hi-.4),(3.2,z-1.5),(1,lo-.2),(-.8,lo-.2)]
            prism("balance_arm",eg,"X",s*(BALANCE_X_CENTER-BALANCE_X_HALF_WIDTH),
                  s*(BALANCE_X_CENTER+BALANCE_X_HALF_WIDTH),arm_profile,"balance")
            box("axlebox_casting",eg,(sx-1.5,-3.1,z-AXLEBOX_HALF_LENGTH,
                                      sx+1.9,3.3,z+AXLEBOX_HALF_LENGTH),"grease",1)
            cylinder("bearing_cover",eg,"X",(s*19.6,0,z),2.9,.8,"steel",caps="cap")
            cylinder("bearing_center",eg,"X",(s*20.1,0,z),1.4,.35,"cap",caps="cap")
            sg = f"frame/suspension/{'left' if s<0 else 'right'}_{index+1}"
            cylinder("lower_spring_plate",sg,"Y",(sx,.9,spring),1.85,.7,"steel")
            spring_length=6.9-FRAME_ATTACHMENT_DROP
            cylinder("spring_hex",sg,"Y",(sx,1.3+spring_length/2,spring),1.5,spring_length,"spring",6,"grease")
            cylinder("upper_spring_plate",sg,"Y",(sx,mount_y(8.5),spring),1.85,.65,"steel")
            # A separate hydraulic damper sits beside, rather than inside, the spring.
            cylinder("damper_body",sg,"Y",(s*19.1,2.55,damper_z),.65,3.2,"grease",6)
            cylinder("damper_piston",sg,"Y",(s*19.1,4.6,damper_z),.29,2,"rod",6)
            for y in (1,mount_y(8.3)):
                cylinder("damper_eye_"+str(y),sg,"X",(s*19.1,y,damper_z),.6,1,"cap",6)
                beam("damper_mount_"+str(y),sg,(sx,y,damper_z),(s*19.1,y,damper_z),1,.65)
        for end in (-1,1):
            bg = f"frame/brake_cylinders/{s}_{end}"
            z = end*34.5
            # Outboard side brackets: cylinder tops remain below the top flange,
            # and the bodies project laterally instead of resting on the beam.
            cx=s*21.4
            x0,x1=sorted((s*18.6,s*19.8))
            box("side_mount_plate",bg,(x0,mount_y(7.3),z-3.1,x1,mount_y(11.7),z+3.1),"steel",.35)
            beam("side_mount_foot",bg,(sx,mount_y(7.8),z),(cx,mount_y(7.8),z),3.7,.7,"grease")
            cylinder("brake_cylinder",bg,"Z",(cx,mount_y(9.7),z),1.6,5,"motor",8,"bolted")
            cylinder("cylinder_end_flange",bg,"Z",(cx,mount_y(9.7),end*37),1.9,.5,"steel",8,"bolted")
            cylinder("push_rod",bg,"Z",(cx,mount_y(9.7),end*38.2),.3,2,"rod",6)
            shaft_z=end*BRAKE_SHAFT_Z
            shaft_joint=(cx,BRAKE_SHAFT_Y,shaft_z)
            beam("brake_end_lever",bg,(cx,mount_y(9.7),end*39.2),shaft_joint,1,.65,"steel")
            cylinder("lever_lower_pin",bg,"X",shaft_joint,.45,1.5,"cap",6)
            # Longer inward consoles carry the outer shoe hanger, as well as the
            # vertical rocker-bearing bracket. Their ends remain disconnected.
            x0,x1=sorted((s*10.8,s*19.1))
            z0,z1=sorted((end*36.8,end*40))
            box("end_console",bg,(x0,mount_y(8),z0,x1,mount_y(11.8),z1),"rail",.35)
            bx0,bx1=sorted((s*16.9,s*18.2))
            prism("brake_rocker_bracket",bg,"X",bx0,bx1,
                  [(y,end*offset) for y,offset in
                   [(BRAKE_BRACKET_BOTTOM,38.95),(BRAKE_BRACKET_BOTTOM,39.75),
                    (BRAKE_BRACKET_BOTTOM+.25,40),(BRAKE_BRACKET_TOP-.25,40),(BRAKE_BRACKET_TOP,39.75),
                    (BRAKE_BRACKET_TOP,38.95),(BRAKE_BRACKET_TOP-.25,38.7),(BRAKE_BRACKET_BOTTOM+.25,38.7)]],"rail")
            bearing=(s*17.55,BRAKE_SHAFT_Y,shaft_z)
            cylinder("rocker_bearing_housing",bg,"X",bearing,.5,1.6,"steel",8,"cap")
            cylinder("brake_rocker_shaft",bg,"X",(s*19.075,BRAKE_SHAFT_Y,shaft_z),.3,6.15,"grease",6)
            # Just a short supported lever and shaft suggest the brake apparatus.
            # No full kinematic reconstruction, actuating cranks or long pull rods.
            BRAKE_MOUNTS.append({"group":bg,"push_rod_end":(cx,mount_y(9.7),end*39.2),
                                 "shaft_joint":shaft_joint,"bearing_center":bearing})
        for z in AXLES:
            for d in (-1,1):
                bg = f"frame/brake_rigging/{s}_{z}_{d}"
                shoe_z = z+d*7
                # Upper-quarter shoes follow the flat of the octagonal tread and
                # keep the brake gear inside the five-block frame envelope.
                profile = [(1.8,z+d*7.35),(1.8,z+d*8),
                           (4.8,z+d*6.75),(4.8,z+d*6.15)]
                prism("brake_shoe",bg,"X",s*13.0,s*15.7,profile,"brake")
                upper_joint=(s*13.1,mount_y(8),z+d*6)
                lower_joint=(s*13.1,3.3,shoe_z)
                beam("shoe_hanger",bg,upper_joint,lower_joint,.65,.65,"grease")
                # Outer hangers use the end consoles; the other hangers have
                # their own shorter supports from the longitudinal side beam.
                if not (abs(z)==32 and d==(-1 if z<0 else 1)):
                    beam("shoe_hanger_console",bg,(sx,mount_y(9.3),z+d*6),(s*13.1,mount_y(9.3),z+d*6),1.5,1.4,"rail")
                # One small textured mounting pad replaces two cheeks and two
                # separate polygonal hinge pins for each shoe hanger.
                box("shoe_hanger_mount",bg,(s*13.1-.8,mount_y(7.6),z+d*6-.6,
                                            s*13.1+.8,mount_y(9.4),z+d*6+.6),"bolted")
    for z in (-16,16):
        # Deep cast/welded transverse element, almost down to the wheel-flange
        # bottom (-8.6px), with tapered lower corners and broad planar walls.
        prism("power_crossmember_"+str(z),"frame/structure","Z",
              z-CROSSMEMBER_HALF_DEPTH,z+CROSSMEMBER_HALF_DEPTH,
              [(-17,FRAME_TOP),(17,FRAME_TOP),(17,2.5),(13,CROSSMEMBER_BOTTOM),
               (-13,CROSSMEMBER_BOTTOM),(-17,2.5)],"rail")
        for s in (-1,1):
            prism("crossmember_gusset_"+str((z,s)),"frame/structure","Z",z-1.8,z+1.8,
                  [(s*11,FRAME_BOTTOM+.4),(s*16.9,FRAME_BOTTOM+.4),(s*16.9,FRAME_TOP)],"steel")
    # Shallow center bolster. The game's stationary half-block occupies the kingpin area.
    box("center_bolster","frame/structure",(-16.3,mount_y(9.7),-3.2,16.3,mount_y(12),3.2),"rail",.6)
    for x in (-7.4,7.4):
        box("flat_body_support_"+str(x),"frame/structure",(x-2.2,mount_y(12),-3,x+2.2,mount_y(12.5),3),"grease",.3)
    # Front and center motor noses face +Z; the rear block is reversed.
    for index,(z,d,cross) in enumerate(((-32,1,-16),(0,1,16),(32,-1,16))):
        mg = "frame/traction_motor_"+str(index+1)
        motor_z = z+d*6.5
        # Large rectangular body. Only the axle-facing end has a coarse rounded
        # profile; the other end, top, bottom and end covers remain flat.
        motor_profile=[(-MOTOR_HALF_HEIGHT,11.8),(MOTOR_HALF_HEIGHT,11.8),
                       (MOTOR_HALF_HEIGHT,3.2),(4.9,.6),(2.3,-1.8),
                       (-2.3,-1.8),(-4.9,.6),(-MOTOR_HALF_HEIGHT,3.2)]
        prism("rectangular_motor_body",mg,"X",-10.4,8.5,
              [(y,z+d*offset) for y,offset in motor_profile],"motor")
        cylinder("pinion_shaft",mg,"X",(9.2,.6,motor_z),1,1.6,"grease",6)
        cover_lo,cover_hi=sorted((z+d*5,z+d*10.4))
        box("terminal_cover",mg,(-8.5,6,cover_lo,5.5,6.65,cover_hi),"bolted",.25)
        inlet_lo,inlet_hi=sorted((z+d*1.6,z+d*4.6))
        box("ventilation_inlet",mg,(-9,5.5,inlet_lo,-5.7,6.9,inlet_hi),"rubber",.3)
        # Non-rotating bearing saddles connect the motor casing to the wheel axle.
        for x in (-5,5):
            cylinder("axle_bearing_"+str(x),mg,"X",(x,0,z),1.6,2,"grease",8)
            beam("bearing_attachment_"+str(x),mg,(x,.7,z),(x,.7,motor_z),2.2,2.3,"motor")
        lo,hi = sorted((z,motor_z))
        prism("gearcase",mg,"X",8.7,11.1,
              [(-3.2,lo-1.8),(-4.1,lo+.4),(-4.1,hi-.4),(-2.3,hi+2.4),
               (2.9,hi+2.4),(3.9,hi-.4),(3.9,lo+.4),(3.2,lo-1.8)],"grease")
        nose_z = z+d*13.7
        lug_lo,lug_hi=sorted((motor_z+d*2.8,nose_z+d*1.2))
        box("nose_lug",mg,(-3.5,1.8,lug_lo,3.5,3.6,lug_hi),"steel",.4)
        beam("nose_support_beam",mg,(-5.5,4,nose_z),(5.5,4,nose_z),1,1.6,"steel")
        for x in (-4.5,4.5):
            cylinder("nose_spring_hex_"+str(x),mg,"Y",(x,5.55,nose_z),.75,2.4,"spring",6,"grease")
            beam("crossmember_suspension_"+str(x),mg,(x,6.9,nose_z),(x,6.9,cross),1.6,1.2,"steel")
            cylinder("nose_mount_pin_"+str(x),mg,"Y",(x,5.45,nose_z),.23,4.6,"rod",6)


def texture():
    image = Image.new("RGB",(ATLAS_SIZE,ATLAS_SIZE),(27,28,27))
    draw = ImageDraw.Draw(image)
    rng = np.random.default_rng(181003)
    for patch in UV_PATCHES:
        name=patch["material"]
        x0,y0=patch["x"],patch["y"]
        x1,y1=x0+patch["width"],y0+patch["height"]
        base = PALETTE[name]
        for y in range(y0-1,y1+1):
            for x in range(x0-1,x1+1):
                # Neutral, low-contrast grain only. Geometry supplies edge
                # lighting; repeated painted scratches look like stray UV marks.
                noise = int(rng.choice([-2,-1,0,0,0,1,2]))
                color = tuple(max(0,min(255,c+noise)) for c in base)
                image.putpixel((x,y),color)
        if name == "spring":
            for y in range(y0,y1):
                # These bands depict the requested coils on a hexagonal body.
                shade = (54,30)[(y-y0)%2]
                draw.line((x0,y,x1-1,y),fill=(shade,shade+1,shade))
        if name in ("cap","wheel") and min(x1-x0,y1-y0)>=4 and abs((x1-x0)-(y1-y0))<=1:
            cx,cy=(x0+x1)/2-.5,(y0+y1)/2-.5
            rr=(x1-x0)/2
            for y in range(y0,y1):
                for x in range(x0,x1):
                    r=math.hypot(x-cx,y-cy)/rr
                    highlight = 4 if .7<r<.83 or .27<r<.38 else -1
                    image.putpixel((x,y),tuple(max(0,c+highlight) for c in base))
            if name == "cap":
                for a in range(0,360,90):
                    x,y = round(cx+math.cos(math.radians(a))*rr*.6),round(cy+math.sin(math.radians(a))*rr*.6)
                    draw.rectangle((x,y,x+1,y+1),fill=(66,67,66))
        if name == "bolted" and min(x1-x0,y1-y0)>=8:
            draw.rectangle((x0+1,y0+1,x1-2,y1-2),outline=(58,59,58))
            draw.rectangle((x0+3,y0+3,x1-4,y1-4),outline=(52,53,52))
            for x in (x0+2,x1-4):
                for y in (y0+2,y1-4):
                    draw.rectangle((x,y,x+1,y+1),fill=(61,62,61))
        # Replicate the border pixels into the one-pixel gutter. Sampling just
        # outside the face's fractional UV rectangle cannot bleed another material.
        for x in range(x0,x1):
            image.putpixel((x,y0-1),image.getpixel((x,y0)))
            image.putpixel((x,y1),image.getpixel((x,y1-1)))
        for y in range(y0-1,y1+1):
            image.putpixel((x0-1,y),image.getpixel((x0,y)))
            image.putpixel((x1,y),image.getpixel((x1-1,y)))
    return image


def mesh_element(p):
    points = np.asarray([v for f in p.faces for v in f.vertices])
    origin = (points.min(axis=0)+points.max(axis=0))/2
    vertices,faces,lookup = {},{},{}
    for i,f in enumerate(p.faces):
        ids=[]
        uv={}
        for vertex,t in zip(f.vertices,f.uv):
            local=tuple(round(x,6) for x in (vec(vertex)-origin))
            if local not in lookup:
                key="v"+str(len(lookup))
                lookup[local]=key
                vertices[key]=local
            key=lookup[local]
            ids.append(key)
            uv[key]=[round(x,5) for x in t]
        faces["f"+str(i)]={"vertices":ids,"uv":uv,"texture":0}
    return {"name":p.name,"type":"mesh","uuid":uid(p.group+"/"+p.name),
            "origin":[round(x,6) for x in origin],"rotation":[0,0,0],
            "vertices":vertices,"faces":faces,"color":0}


def write_bbmodel(image):
    elements=[mesh_element(p) for p in PARTS]
    groups={}
    root=[]
    for p,e in zip(PARTS,elements):
        siblings=root
        path=""
        for name in p.group.split("/"):
            path+="/"+name
            if path not in groups:
                g={"name":name,"origin":[0,0,0],"rotation":[0,0,0],"uuid":uid("group"+path),
                   "export":True,"isOpen":False,"children":[]}
                groups[path]=g
                siblings.append(g)
            siblings=groups[path]["children"]
        siblings.append(e["uuid"])
    model={"meta":{"format_version":"4.10","model_format":"free","box_uv":False},
           "name":"Triple-axle equalized locomotive bogey / v2",
           "model_identifier":"railward:"+NAME,
           "resolution":{"width":ATLAS_SIZE,"height":ATLAS_SIZE},
           "elements":elements,"outliner":root,
           "textures":[{"name":NAME+".png","path":"./"+NAME+".png","relative_path":"./"+NAME+".png",
                        "uuid":uid("texture"),"id":"0","particle":True,"namespace":"railward",
                        "width":ATLAS_SIZE,"height":ATLAS_SIZE,"uv_width":ATLAS_SIZE,"uv_height":ATLAS_SIZE,
                        "saved":True,"render_mode":"default","render_sides":"auto","internal":True,
                        "source":"data:image/png;base64,"+base64.b64encode((OUT/(NAME+".png")).read_bytes()).decode()}]}
    (OUT/(NAME+".bbmodel")).write_text(json.dumps(model,ensure_ascii=False,indent=2),encoding="utf-8")
    return model


def write_obj(component,parts,game_uv):
    # Frame renderer Y=12px, wheel renderer Y=13.5px. Match the existing wheel-center datum.
    delta = {"frame":vec((0,1.5,0)),"wheelset":vec((0,0,32)),"assembled":vec((0,0,0))}[component]
    obj=["# Railward editable candidate v2; positions in blocks",f"mtllib {NAME}_{component}.mtl"]
    vertex_index=1
    for p in parts:
        # Forge stores named OBJ objects in a map. Repeated names discard entire
        # earlier objects, so the outliner path is part of every exported name.
        obj.append("o "+(p.group+"/"+p.name).replace("/",".").replace(" ","_"))
        for f in p.faces:
            normal=unit(np.cross(vec(f.vertices[1])-f.vertices[0],vec(f.vertices[2])-f.vertices[0]))
            obj.append("usemtl "+f.material)
            for v in f.vertices:
                obj.append("v "+" ".join(f"{x:.7f}" for x in (vec(v)+delta)/16))
            for u,v in f.uv:
                # OBJ uses V-up. Forge flip-v=true converts back to the atlas's
                # top-left convention; both export flavors retain standard OBJ UVs.
                obj.append(f"vt {u/ATLAS_SIZE:.7f} {(ATLAS_SIZE-v)/ATLAS_SIZE:.7f}")
            for _ in f.vertices:
                obj.append("vn "+" ".join(f"{x:.7f}" for x in normal))
            obj.append("f "+" ".join(f"{k}/{k}/{k}" for k in range(vertex_index,vertex_index+len(f.vertices))))
            vertex_index+=len(f.vertices)
    folder=OUT/("forge" if game_uv else "portable")
    folder.mkdir(exist_ok=True)
    (folder/f"{NAME}_{component}.obj").write_text("\n".join(obj)+"\n",encoding="utf-8")
    mtl=[]
    for mat in TILES:
        mtl.extend(["newmtl "+mat,"Ka 0 0 0","Kd 1 1 1","Ks 0 0 0",
                    "map_Kd "+("#texture" if game_uv else "../"+NAME+".png"),""])
    (folder/f"{NAME}_{component}.mtl").write_text("\n".join(mtl),encoding="utf-8")
    if game_uv:
        # Use Forge 40/43 names in shared resources. processResources translates
        # these keys to Forge 47's schema for both 1.20.1/Create-major targets.
        wrapper={"parent":"block/block","loader":"forge:obj","flip-v":True,
                 "detectCullableFaces":False,"diffuseLighting":True,"ambientToFullbright":False,
                 "model":f"railward:models/block/train/bogey/triple_axle_equalized_locomotive/{NAME}_{component}.obj",
                 "textures":{"texture":"railward:block/train/bogey/"+NAME,"particle":"railward:block/train/bogey/"+NAME}}
        (folder/f"{NAME}_{component}.json").write_text(json.dumps(wrapper,indent=2),encoding="utf-8")


def install_game_resources():
    """Publish validated exports under stable IDs without changing bogey physics."""
    assets=ROOT/"src/main/resources/assets/railward"
    models=assets/"models/block/train/bogey/triple_axle_equalized_locomotive"
    textures=assets/"textures/block/train/bogey"
    models.mkdir(parents=True,exist_ok=True)
    textures.mkdir(parents=True,exist_ok=True)
    for component in ("frame","wheelset"):
        for extension in ("obj","mtl","json"):
            source=OUT/"forge"/f"{NAME}_{component}.{extension}"
            destination=models/f"{GAME_NAME}_{component}.{extension}"
            destination.write_text(source.read_text(encoding="utf-8").replace(NAME,GAME_NAME),encoding="utf-8")
    shutil.copyfile(OUT/(NAME+".png"),textures/(GAME_NAME+".png"))
    print("Installed frame, centered wheelset and atlas in shared game resources")


def render(image,camera,size,clay=False,groups=None,part_filter=None,diagram=False):
    """Orthographic textured z-buffer of the ACTUAL mesh, with one-sided faces."""
    w,h=size
    pixels=np.full((h,w,3),(222,220,210),dtype=np.uint8)
    depth=np.full((h,w),-np.inf)
    target=vec((0,3.5,0))
    toward=unit(camera)
    helper=vec((0,1,0)) if abs(toward[1])<.98 else vec((0,0,-1))
    right=unit(np.cross(helper,toward))
    up=unit(np.cross(toward,right))
    selected=[p for p in PARTS if (groups is None or any(p.group.startswith(g) for g in groups))
              and (part_filter is None or part_filter(p))]
    allpoints=np.asarray([v for p in selected for f in p.faces for v in f.vertices])-target
    projected=np.stack((allpoints@right,allpoints@up),axis=1)
    lo,hi=projected.min(axis=0),projected.max(axis=0)
    scale=min((w-60)/(hi[0]-lo[0]),(h-60)/(hi[1]-lo[1]))
    offset=(lo+hi)/2
    atlas=np.asarray(image)
    light=unit((-.35,.9,-.5))
    for p in selected:
        for face in p.faces:
            verts=np.asarray(face.vertices)
            normal=unit(np.cross(verts[1]-verts[0],verts[2]-verts[0]))
            if np.dot(normal,toward)<=1e-8:
                continue
            q=verts-target
            screen=np.stack(((q@right-offset[0])*scale+w/2,
                             -(q@up-offset[1])*scale+h/2,q@toward),axis=1)
            shade=.67+.33*max(0,float(normal@light))
            if clay:
                # Parts remain separable in the construction sheet, with neutral greys.
                col=(138,143,139) if "wheelset" in p.group else (111,121,121)
                if "suspension" in p.group: col=(156,143,114)
                if "traction" in p.group: col=(121,135,141)
                if "equalizer" in p.group or p.name.startswith("pivot_pin_"): col=(74,125,158)
                if "balance_hinge_bracket" in p.name: col=(182,113,57)
                if diagram and "wheelset" in p.group: col=(221,188,67)
                if diagram and p.name in ("longitudinal_beam","top_flange"): col=(86,166,72)
            for i in range(1,len(verts)-1):
                ids=[0,i,i+1]
                triangle=screen[ids]
                uv=np.asarray(face.uv)[ids]
                xmin=max(0,int(np.floor(triangle[:,0].min())))
                xmax=min(w-1,int(np.ceil(triangle[:,0].max())))
                ymin=max(0,int(np.floor(triangle[:,1].min())))
                ymax=min(h-1,int(np.ceil(triangle[:,1].max())))
                if xmin>xmax or ymin>ymax: continue
                xx,yy=np.meshgrid(np.arange(xmin,xmax+1)+.5,np.arange(ymin,ymax+1)+.5)
                a,b,c=triangle
                denom=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
                if abs(denom)<1e-9: continue
                wa=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/denom
                wb=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/denom
                wc=1-wa-wb
                zz=wa*a[2]+wb*b[2]+wc*c[2]
                local_depth=depth[ymin:ymax+1,xmin:xmax+1]
                mask=(wa>=-1e-7)&(wb>=-1e-7)&(wc>=-1e-7)&(zz>local_depth)
                if not mask.any(): continue
                if clay:
                    sampled=np.full((*xx.shape,3),col)
                else:
                    uu=wa*uv[0,0]+wb*uv[1,0]+wc*uv[2,0]
                    vv=wa*uv[0,1]+wb*uv[1,1]+wc*uv[2,1]
                    sampled=atlas[np.clip(vv.astype(int),0,ATLAS_SIZE-1),np.clip(uu.astype(int),0,ATLAS_SIZE-1)]
                colors=np.clip(sampled*shade,0,255).astype(np.uint8)
                pixels[ymin:ymax+1,xmin:xmax+1][mask]=colors[mask]
                local_depth[mask]=zz[mask]
    return Image.fromarray(pixels)


def font(size):
    try: return ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf",size)
    except OSError: return ImageFont.load_default()


def previews(atlas):
    hero=render(atlas,(7,4.8,-8),(1600,1000))
    hero.save(OUT/"preview_perspective.png")
    render(atlas,(1,0,0),(1600,620)).save(OUT/"preview_side.png")
    proportion_view=Image.new("RGB",(1600,720),(238,236,228))
    proportion_view.paste(render(atlas,(1,0,0),(1600,610),True,diagram=True),(0,55))
    proportion_draw=ImageDraw.Draw(proportion_view)
    proportion_draw.text((35,12),"Пропорции: рама перекрывает верхнюю часть колёс",font=font(30),fill=(41,45,43))
    proportion_draw.text((35,48),"Пунктир — верхний контур колёс, скрытый за рамой",font=font(20),fill=(73,78,70))
    # Expose only the occluded upper tread outline, using the exact projection
    # of the renderer above. This makes the frame/wheel overlap comparable to
    # the user's annotated photo without changing the actual model render.
    points=np.asarray([v for p in PARTS for f in p.faces for v in f.vertices])
    scale=min((1600-60)/np.ptp(points[:,2]),(610-60)/np.ptp(points[:,1]))
    mid_y=(points[:,1].min()+points[:,1].max())/2
    for z in AXLES:
        for i in range(8):
            a,b=i*math.tau/8,(i+1)*math.tau/8
            pa=vec((8*math.cos(a),z+8*math.sin(a)))
            pb=vec((8*math.cos(b),z+8*math.sin(b)))
            for k in range(0,20,2):
                p0=pa+(pb-pa)*k/20
                p1=pa+(pb-pa)*(k+1)/20
                if min(p0[0],p1[0])<FRAME_BOTTOM: continue
                xy0=(800-p0[1]*scale,55+305-(p0[0]-mid_y)*scale)
                xy1=(800-p1[1]*scale,55+305-(p1[0]-mid_y)*scale)
                proportion_draw.line((xy0,xy1),fill=(243,217,91),width=3)
    x=35
    for label,color in (("Рама",(86,166,72)),("Кронштейны",(182,113,57)),
                        ("Балансиры",(74,125,158)),("Колёса",(221,188,67))):
        proportion_draw.rectangle((x,678,x+18,696),fill=color)
        proportion_draw.text((x+28,669),label,font=font(25),fill=(41,45,43))
        x+=360
    proportion_view.save(OUT/"preview_proportions.png")
    render(atlas,(0,1,0),(1050,1500)).save(OUT/"preview_top.png")
    render(atlas,(6,-7,-6),(1400,1000)).save(OUT/"preview_underside.png")
    render(atlas,(7,3,-8),(1300,950),True,
           groups=["frame/brake_cylinders/1_1","frame/brake_rigging/1_32_1",
                   "frame/suspension/right_3","frame/equalizers/right_3",
                   "wheelset_rear"]).save(OUT/"preview_brake_detail.png")
    def hinge_parts(p):
        return ((p.group=="frame/side_right" and
                 (p.name.startswith("shared_balance_hinge_bracket") or p.name in ("pivot_pin_1","pivot_pin_2")))
                or p.group in ("frame/equalizers/right_1","frame/equalizers/right_2"))
    render(atlas,(7,-2,-6),(1400,850),True,
           part_filter=hinge_parts).save(OUT/"preview_balance_detail.png")
    sheet=Image.new("RGB",(1800,1500),(238,236,228))
    sheet.paste(hero.resize((1000,625)),(30,100))
    sheet.paste(render(atlas,(1,0,0),(720,460)),(1060,200))
    sheet.paste(render(atlas,(6,-7,-6),(1000,650),True),(30,770))
    sheet.paste(render(atlas,(0,1,0),(720,630),True),(1060,770))
    draw=ImageDraw.Draw(sheet)
    draw.text((40,22),"Трёхосная балансирная локомотивная тележка · v2",font=font(38),fill=(41,45,43))
    for xy,text_ in [((40,78),"Фактическая геометрия и текстура"),((1080,158),"Вид сбоку"),
                     ((40,740),"Конструкция снизу"),((1080,740),"Расположение моторов и поперечин")]:
        draw.text(xy,text_,font=font(24),fill=(70,75,69))
    draw.text((40,1430),"5 блоков · оси −32 / 0 / +32 px · колесо Ø16 px · пружины 6 граней · шкворня нет",font=font(24),fill=(70,75,69))
    sheet.save(OUT/"preview_sheet.png")


def validate(model):
    failures=[]
    uv_anisotropy=[]
    uv_density=defaultdict(list)
    identities=[e["uuid"] for e in model["elements"]]
    if len(identities)!=len(set(identities)): failures.append("duplicate mesh UUID")
    count=Counter()
    for p,e in zip(PARTS,model["elements"]):
        # Every primitive is a closed, consistently wound surface. Welded vertices also
        # make Face/Edge/Vertex edits practical in Blockbench.
        edges=defaultdict(list)
        for f in e["faces"].values():
            ids=f["vertices"]
            for a,b in zip(ids,ids[1:]+ids[:1]): edges[tuple(sorted((a,b)))].append((a,b))
        for edge,uses in edges.items():
            if len(uses)!=2 or uses[0]!=uses[1][::-1]:
                failures.append(f"{p.group}/{p.name}: open or inconsistent edge {edge}")
        for f in p.faces:
            if len(f.vertices) not in (3,4): failures.append("Forge-incompatible polygon")
            n=np.cross(vec(f.vertices[1])-f.vertices[0],vec(f.vertices[2])-f.vertices[0])
            if np.linalg.norm(n)<1e-8: failures.append(f"degenerate face: {p.name}")
            if any(not(0<=u<=ATLAS_SIZE and 0<=v<=ATLAS_SIZE) for u,v in f.uv): failures.append("UV outside atlas")
        # Check the SAVED Blockbench mesh data, including rounding. A UV triangle's
        # two singular scales must match: unequal scales produce elongated pixels.
        for source,saved in zip(p.faces,e["faces"].values()):
            ids=saved["vertices"]
            points=np.asarray([e["vertices"][v] for v in ids])
            coords=np.asarray([saved["uv"][v] for v in ids])
            for i in range(1,len(ids)-1):
                spatial=np.stack((points[i]-points[0],points[i+1]-points[0]),axis=1)
                _,physical=np.linalg.qr(spatial,mode="reduced")
                mapped=np.stack((coords[i]-coords[0],coords[i+1]-coords[0]),axis=1)
                scales=np.linalg.svd(mapped@np.linalg.inv(physical),compute_uv=False)
                ratio=float(scales[0]/max(scales[1],1e-12))
                uv_anisotropy.append(ratio)
                uv_density[source.material].extend(float(s) for s in scales)
                if ratio>1.002 or abs(scales.mean()-material_density(source.material))>.002:
                    failures.append(f"stretched UV pixels: {p.group}/{p.name} ratio={ratio:.4f}")
        volume = sum(float(np.dot(vec(f.vertices[0]),np.cross(vec(f.vertices[i]),vec(f.vertices[i+1]))))/6
                     for f in p.faces for i in range(1,len(f.vertices)-1))
        if volume <= 1e-8: failures.append(f"inward or zero-volume surface: {p.group}/{p.name}")
        for key,local in e["vertices"].items():
            absolute=vec(local)+e["origin"]
            if not any(np.linalg.norm(absolute-v)<2e-6 for f in p.faces for v in f.vertices):
                failures.append("Blockbench local origin mismatch")
        count["parts"]+=1
        count["faces"]+=len(p.faces)
    framepoints=np.asarray([v for p in PARTS if p.group.startswith("frame") for f in p.faces for v in f.vertices])
    if framepoints[:,2].min() < -40.001 or framepoints[:,2].max()>40.001:
        failures.append("frame exceeds five-block longitudinal envelope")
    # Acceptance checks on actual geometry, including the later layout refinement.
    hinge_positions={}
    for side in ("left","right"):
        pins=[p for p in PARTS if p.group=="frame/side_"+side and p.name.startswith("pivot_pin_")]
        positions=[float(np.mean([v[2] for f in p.faces for v in f.vertices])) for p in pins]
        if len(positions)!=3 or len(set(round(z,3) for z in positions))!=3:
            failures.append("balance arms do not have three distinct frame hinges on "+side)
        hinge_positions[side]=positions
    if any(p.name.startswith("end_crossmember") for p in PARTS):
        failures.append("closed end crossmember remains")
    cylinders=[p for p in PARTS if p.name=="brake_cylinder"]
    if len(cylinders)!=4: failures.append("expected four side brake cylinders")
    for p in cylinders:
        points=np.asarray([v for f in p.faces for v in f.vertices])
        if np.abs(points[:,0]).min()<19.2 or points[:,1].max()>FRAME_TOP+.3:
            failures.append("brake cylinder is not on an outboard side bracket")
    crossmembers=[p for p in PARTS if p.name.startswith("power_crossmember_")]
    if len(crossmembers)!=2: failures.append("expected two power crossmembers")
    for p in crossmembers:
        points=np.asarray([v for f in p.faces for v in f.vertices])
        if points[:,1].min()>-6 or not 2.9<=np.ptp(points[:,2])<=3.1:
            failures.append("power crossmember has incorrect depth or height")
    motors=[p for p in PARTS if p.name=="rectangular_motor_body"]
    if len(motors)!=3 or any(p.name=="octagonal_motor_body" for p in PARTS):
        failures.append("expected three box-shaped motor bodies")
    for p in motors:
        points=np.asarray([v for f in p.faces for v in f.vertices])
        if np.ptp(points[:,0])<18 or np.ptp(points[:,2])<13 or np.ptp(points[:,1])<11.9:
            failures.append("traction motor body is too small or vertically thin")
    by_name={(p.group,p.name):p for p in PARTS}
    def contains(p,point,tolerance=.03):
        # All checked supports/links are convex closed primitives. Outward face
        # planes verify contact with the real surfaces, not only their bounding boxes.
        for f in p.faces:
            n=unit(np.cross(vec(f.vertices[1])-f.vertices[0],vec(f.vertices[2])-f.vertices[0]))
            if n@(vec(point)-f.vertices[0])>tolerance:
                return False
        return True
    def require_contact(group,names,point):
        for name in names:
            p=by_name.get((group,name))
            if p is None or not contains(p,point):
                failures.append(f"unsupported joint: {group}/{name} at {point}")
    shared_brackets=[p for p in PARTS if p.name=="shared_balance_hinge_bracket"]
    if len(shared_brackets)!=2: failures.append("expected one shared hinge apron per side")
    for s,side in ((-1,"left"),(1,"right")):
        group="frame/side_"+side
        for i,(axle,pivot,spring,damper) in enumerate(BALANCE_LAYOUT):
            bracket="shared_balance_hinge_bracket" if i<2 else "rear_balance_hinge_bracket"
            pin="pivot_pin_"+str(i+1)
            require_contact(group,[bracket,pin],(s*19.8,BALANCE_PIVOT_Y,pivot))
            require_contact(group,[bracket+"_inner_cheek",pin],(s*16.6,BALANCE_PIVOT_Y,pivot))
            require_contact(group,[bracket,bracket+"_bridge"],(s*19.8,HINGE_BRACKET_TOP-.35,pivot))
            arm=by_name[(f"frame/equalizers/{side}_{i+1}","balance_arm")]
            axis_center=(s*BALANCE_X_CENTER,BALANCE_PIVOT_Y,pivot)
            if not contains(arm,axis_center): failures.append("balance arm does not enter its hinge fork")
            arm_points=np.asarray([v for f in arm.faces for v in f.vertices])
            if abs((arm_points[:,1].min()+arm_points[:,1].max())/2-BALANCE_PIVOT_Y)>.01:
                failures.append("balance hinge is not on the vertical arm center")
            inner=by_name[(group,bracket+"_inner_cheek")]
            outer=by_name[(group,bracket)]
            inner_x=np.abs([v[0] for f in inner.faces for v in f.vertices])
            outer_x=np.abs([v[0] for f in outer.faces for v in f.vertices])
            arm_x=np.abs(arm_points[:,0])
            if arm_x.min()-inner_x.max()<.3 or outer_x.min()-arm_x.max()<.3:
                failures.append("balance arm is not inside the wider bracket slot")
            if not abs(spring-axle)<abs(damper-axle): failures.append("spring is outside the hydraulic damper")
            if abs(spring-axle)-1.5-AXLEBOX_HALF_LENGTH<.09:
                failures.append("spring intersects the axlebox casting")
    removed=("longitudinal_brake_rod","brake_air_pipe","pull_rod_crank","pull_rod_joint",
             "shoe_actuating_crank","outer_shoe_link","shoe_pull_link","shoe_upper_pin","shoe_lower_pin")
    if any(p.name.startswith(removed) for p in PARTS): failures.append("excess brake linkage detail remains")
    if len(BRAKE_MOUNTS)!=4: failures.append("expected four short supported visual brake mounts")
    for chain in BRAKE_MOUNTS:
        group=chain["group"]
        require_contact(group,["push_rod","brake_end_lever"],chain["push_rod_end"])
        require_contact(group,["brake_end_lever","brake_rocker_shaft"],chain["shaft_joint"])
        require_contact(group,["brake_rocker_bracket","rocker_bearing_housing","brake_rocker_shaft"],chain["bearing_center"])
        s,end=(int(x) for x in group.rsplit("/",1)[1].split("_"))
        require_contact(group,["end_console","brake_rocker_bracket"],(s*17.55,mount_y(9.4),end*39))
        shoe_group=f"frame/brake_rigging/{s}_{end*32}_{end}"
        check=(s*13.1,mount_y(9.1),end*38)
        require_contact(group,["end_console"],check)
        require_contact(shoe_group,["shoe_hanger_mount"],check)
        bracket=by_name[(group,"brake_rocker_bracket")]
        points=np.asarray([v for f in bracket.faces for v in f.vertices])
        if abs(np.ptp(points[:,1])-8.2)>.01: failures.append("brake bracket has not been shortened by half")
    for side in ("left","right"):
        rail=by_name[("frame/side_"+side,"longitudinal_beam")]
        points=np.asarray([v for f in rail.faces for v in f.vertices])
        bottom,top=points[:,1].min(),points[:,1].max()
        if not bottom<8<top or abs(top-bottom-5)>.01:
            failures.append("frame does not overlap the upper wheel region with the requested deeper section")
        for i,(_,_,spring,damper) in enumerate(BALANCE_LAYOUT):
            s=-1 if side=="left" else 1
            group=f"frame/suspension/{side}_{i+1}"
            point=(s*17.5,mount_y(8.5),spring)
            require_contact("frame/side_"+side,["longitudinal_beam"],point)
            require_contact(group,["upper_spring_plate"],point)
            require_contact("frame/side_"+side,["longitudinal_beam"],(s*17.5,mount_y(8.3),damper))
    # Keep the tiny end-brake shaft below the hydraulic damper's lower eye.
    if BRAKE_SHAFT_Y+.3>=1-.6:
        failures.append("lowered end-brake shaft intersects the hydraulic damper eye")
    if failures:
        raise RuntimeError("\n".join(failures[:20]))
    uv_report={"checked_triangles":len(uv_anisotropy),"max_pixel_aspect_ratio":max(uv_anisotropy),
               "isotropic":True,"density_px_per_model_px":
               {m:{"min":min(v),"max":max(v)} for m,v in sorted(uv_density.items())},
               "material_patches":len(UV_PATCHES),"atlas_size":ATLAS_SIZE}
    (OUT/"uv_validation.json").write_text(json.dumps(uv_report,indent=2),encoding="utf-8")
    report={"checks":"closed surfaces; consistent outward winding and positive volume; finite triangle/quad faces; welded vertices; local origins; UV bounds; five-block frame",
            **count,"texture_size":ATLAS_SIZE,"axle_centers_px":AXLES,"tread_diameter_px":16,
            "frame_bounds_px":[framepoints.min(axis=0).tolist(),framepoints.max(axis=0).tolist()],
            "spring_segments":6,"motor_nose_directions":["+Z","+Z","-Z"],
            "frame_proportions":{"main_beam_bottom_px":FRAME_BOTTOM,"main_beam_top_px":FRAME_TOP,
                                 "main_beam_height_px":FRAME_TOP-FRAME_BOTTOM,
                                 "upper_wheel_overlap_px":8-FRAME_BOTTOM,
                                 "hinge_bracket_top_px":HINGE_BRACKET_TOP,
                                 "upper_attachment_drop_px":FRAME_ATTACHMENT_DROP},
            "reference_corrections":{"distinct_hinge_positions_px":hinge_positions,
                                     "end_crossmembers":0,"open_end_consoles":4,
                                     "outboard_side_brake_cylinders":len(cylinders),
                                     "power_crossmember_depth_px":2*CROSSMEMBER_HALF_DEPTH,
                                     "power_crossmember_bottom_px":CROSSMEMBER_BOTTOM,
                                     "rectangular_motor_bodies":len(motors),
                                     "motor_body_height_px":2*MOTOR_HALF_HEIGHT,
                                     "shared_balance_hinge_brackets":len(shared_brackets),
                                     "balance_arm_width_px":2*BALANCE_X_HALF_WIDTH,
                                     "hinge_fork_slot_width_px":BRACKET_OUTER_X[0]-BRACKET_INNER_X[1],
                                     "balance_hinge_vertical_center_px":BALANCE_PIVOT_Y,
                                     "spring_inside_damper":True,
                                     "short_visual_brake_mounts":len(BRAKE_MOUNTS),
                                     "brake_bracket_height_px":8.2,
                                     "long_brake_rods_and_air_pipes":0},
            "game_resource_id":"railward:"+GAME_NAME}
    (OUT/"validation.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
    print(json.dumps(report,ensure_ascii=False))


def validate_exports():
    """Parse the files on disk, including Forge's V flip, not just source meshes."""
    report={}
    for path in sorted(OUT.glob("*/*.obj")):
        vertices,uvs,normals,objects,faces=[],[],[],[],[]
        for line in path.read_text(encoding="utf-8").splitlines():
            tokens=line.split()
            if not tokens: continue
            if tokens[0] in ("v","vt","vn"):
                {"v":vertices,"vt":uvs,"vn":normals}[tokens[0]].append([float(t) for t in tokens[1:]])
            elif tokens[0]=="o": objects.append(tokens[1])
            elif tokens[0]=="f": faces.append([[int(t)-1 for t in v.split("/")] for v in tokens[1:]])
        if len(objects)!=len(set(objects)):
            raise RuntimeError("duplicate exported object name: "+str(path))
        for face in faces:
            if len(face) not in (3,4): raise RuntimeError("invalid OBJ polygon")
            p=[vec(vertices[v[0]]) for v in face]
            normal=unit(np.cross(p[1]-p[0],p[2]-p[0]))
            for v in face:
                if normal@unit(normals[v[2]]) < .99999: raise RuntimeError("OBJ normal mismatch")
                u,w=uvs[v[1]]
                if not(0<=u<=1 and 0<=w<=1): raise RuntimeError("OBJ UV range mismatch")
        component=path.stem.removeprefix(NAME+"_")
        expected=[p for p in PARTS if component=="assembled" or
                  (component=="frame" and p.group.startswith("frame")) or
                  (component=="wheelset" and p.group=="wheelset_front")]
        if len(faces)!=sum(len(p.faces) for p in expected): raise RuntimeError("OBJ lost faces")
        uv_index=0
        for p in expected:
            for f in p.faces:
                for u,v in f.uv:
                    actual_u,actual_v=uvs[uv_index]
                    # Both standard viewers and Forge flip-v must recover the same top-left pixels.
                    if abs(actual_u*ATLAS_SIZE-u)>1e-4 or abs((1-actual_v)*ATLAS_SIZE-v)>1e-4:
                        raise RuntimeError("OBJ atlas convention mismatch")
                    uv_index+=1
        report[str(path.relative_to(OUT))]={"objects":len(objects),"faces":len(faces),
                                           "unique_names":True,"normals":True,"UV_roundtrip":True}
    (OUT/"export_validation.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
    print("OBJ roundtrip checks passed for "+str(len(report))+" files")


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--no-preview",action="store_true")
    parser.add_argument("--install-resources",action="store_true",
                        help="replace the shared game resources with validated frame/wheelset exports")
    args=parser.parse_args()
    OUT.mkdir(parents=True,exist_ok=True)
    build_geometry()
    remap_uvs()
    atlas=texture()
    atlas.save(OUT/(NAME+".png"))
    model=write_bbmodel(atlas)
    validate(model)
    for game_uv in (False,True):
        write_obj("frame",[p for p in PARTS if p.group.startswith("frame")],game_uv)
        write_obj("wheelset",[p for p in PARTS if p.group=="wheelset_front"],game_uv)
    write_obj("assembled",PARTS,False)
    validate_exports()
    if args.install_resources: install_game_resources()
    if not args.no_preview: previews(atlas)
    print("Saved candidate to "+str(OUT))


if __name__=="__main__":
    main()
