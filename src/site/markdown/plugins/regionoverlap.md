Region Overlap
==============

Measure the overlapping area between two sets of
regions of interest (ROI). Compares pairs of ROIs
and returns the overlap and non-overlap areas.

Input Parameters
----------------

#### Region A

First set of ROIs to compare. Region A and Region B should have 
the same number of ROIs, which will be compared as ordered pairs.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)

#### Region B

Second set of ROIs to compare. Region A and Region B should have 
the same number of ROIs, which will be compared as ordered pairs.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)

Results
-------

#### Overlap

For each ROI pair, returns the number of pixels 
contained within the union of ROI A and ROI B.

Type: Numeric (`double`)

#### A Outside B

For each ROI pair, returns the number of pixels
contained within ROI A and outside of ROI B.

Type: Numeric (`double`)

#### B Outside A

For each ROI pair, returns the number of pixels
contained within ROI B and outside of ROI A.

Type: Numeric (`double`)
