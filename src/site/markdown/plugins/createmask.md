Create Mask Image
=================

Generate a mask image from a region of interest
(ROI) set. Pixels within one of the ROIs are
set to `255`. Pixels outside the ROIs are set to `0`.

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

Raster representation of `Regions`. Pixels within
an ROI are set to `255`. Pixels outside the ROIs
are set to `0`. Image dimensions are determined
by the dimensions of `Template`, and ROIs outside
of those dimensions are ignored.

Type: Image File (`imagej.data.Dataset`)
