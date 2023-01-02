Count Intersections
===================

Count the number of intersections between region of interest
(ROI) borders and a binary mask.

### Input parameters

#### ROIs

Set of line, polyline, or polygon ROIs. Other ROIs are not
supported.

Type: ROI Set (file) (`ij.gui.Roi[]`)

#### Image

Binary mask image.

Type: Image File (`ij.ImagePlus`)

#### Invert Image

If `true`, treat the image as an inverted mask.

Type: Logical (`Boolean`)

#### Dilate Mask

If `true`, dilate the mask image. Recommended for thin elements.

Type: Logical (`Boolean`)

### Results

One result row is produced for each ROI.

#### Intersections

Number of intersections between the ROI border and the binary
mask. An intersection is defined as the ROI border crossing
from image value `0` to `1`, then back to `0`.

Type: Numeric (`double`)

#### Length-Ends

Length of the entire ROI border, in pixels.

Type: Numeric (`double`)

#### Length-Intersections

Length of the ROI border, trimmed to the outermost mask 
intersections, in pixels.

Type: Numeric (`double`)
