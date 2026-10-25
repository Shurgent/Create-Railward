# Project screenshots and animations

Screenshots and GIFs for the README and external project descriptions live in
`features/`. They are documentation assets and are not packaged in the mod.

Keep filenames stable once they are used on CurseForge or another external
site. Add a new filename when an older image needs to remain available.

## Embedding images

In this repository's root README, use relative paths:

```markdown
![Automatic train coupling](docs/images/features/automatic-coupling.gif)
```

For a CurseForge description in Markdown mode, use a direct HTTPS image URL:

```markdown
![Automatic train coupling](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/automatic-coupling.gif)
```

In the visual editor, use the same direct URL as the image source. A GitHub
`blob/` page URL opens the file viewer rather than serving the image itself.

**External embeds require a public repository.** While this repository is
private, these URLs do not provide anonymous access for CurseForge visitors.
Making the repository public later will make the same image URLs usable.

The URLs below follow `main`, so replacing an image updates what external pages
display. To pin an image to an exact revision, replace `main` in its URL with
the full commit SHA containing that image.

## Direct image URLs

| Image | Direct URL |
| --- | --- |
| 18-100 freight bogey under a wagon | [bogey-18-100.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/bogey-18-100.png) |
| 18-100 freight bogey detail | [bogey-18-100-detail.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/bogey-18-100-detail.png) |
| Triple-axle bogey under a locomotive | [bogey-triple-axle.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/bogey-triple-axle.png) |
| Triple-axle bogey detail | [bogey-triple-axle-detail.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/bogey-triple-axle-detail.png) |
| Bulk-cargo loading | [bulk-cargo.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/bulk-cargo.png) |
| Modern train controller | [controller.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/controller.png) |
| Controller with reverser set to backward | [controller-reversed.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/controller-reversed.png) |
| Manual train decoupling | [coupler.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/coupler.png) |
| Modern railway signals | [signals.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/signals.png) |
| Locomotive with diesel exhaust | [diesel-exhaust.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/diesel-exhaust.png) |
| Projected locomotive spotlight | [spotlight.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/spotlight.png) |
| Cistern connected to a fluid terminal | [cistern.png](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/cistern.png) |
| Automatic train coupling | [automatic-coupling.gif](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/automatic-coupling.gif) |
| Direction-dependent marker lights | [marker-lights.gif](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/marker-lights.gif) |
| Diesel exhaust on a moving train | [diesel-exhaust.gif](https://raw.githubusercontent.com/Shurgent/Create-Railward/main/docs/images/features/diesel-exhaust.gif) |

CurseForge supports Markdown descriptions and embedded images; see its
[project submission guide](https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips).
GitHub recommends relative paths for images within a repository; see
[the README documentation](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes#relative-links-and-image-paths-in-markdown-files).
