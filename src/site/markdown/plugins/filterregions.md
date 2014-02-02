Filter Regions
==============

Filter one region of interest (ROI) set
using a second ROI set as a mask.

Input Parameters
----------------

#### Regions

ROIs to filter.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)

#### Mask

ROIs to use as a mask.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)

Results
-------

#### Filtered regions

Subset of ROIs in `Regions` where each integer coordinate
that lies within the ROI also lies within at least one
of the ROIs in `Mask`.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)
