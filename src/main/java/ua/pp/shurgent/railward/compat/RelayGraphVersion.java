package ua.pp.shurgent.railward.compat;

/** Topology revision, independent of Create's signal groups and save-dirty flag. */
public interface RelayGraphVersion {
	long railward$getRelayRevision();
}
