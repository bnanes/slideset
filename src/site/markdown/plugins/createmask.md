Create Mask Image
=================

Generate a mask image from a region of interest (ROI) set. Two versions
of this command are available. For the `Create Mask Image` command,
pixels within any one of the ROIs are set to `255` and pixels outside all
of the ROIs are set to `0`. For the `Create Multimask Image` command,
pixel values are set to the index value of the containing ROI, or `0` for
pixels outside all ROIs.

Input Parameters
----------------

#### Regions

Regions defining the mask.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)

#### Template

Image file whose dimensions will be used as a
template for the generated mask images.

Type: Image File (`imagej.data.Dataset`)

Results
-------

#### Mask image

Raster representation of `Regions`. Result depends on the command version.

For the `Create Mask Image` command:

Pixels within an ROI are set to `255`. Pixels outside the ROIs
are set to `0`.

For the `Create Multimask Image` command:

Pixels within an ROI are set to the 1-based index of the ROI within the ROI set.
Pixels within multiple ROIs are set to the highest index value of all the
containing ROIs. Pixels outside of all ROIs are set to `0`. If the ROI set
contains more than 255 regions, pixels in higher-index regions will be set
to `255`.

Image dimensions are determined by the dimensions of `Template`, 
and ROIs outside of those dimensions are ignored.

Type: Image File (`imagej.data.Dataset`)
