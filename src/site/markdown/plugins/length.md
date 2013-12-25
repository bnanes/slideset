ROI Lengths
===========

Calculate lengths of line-based regions of interest (ROIs).

Input Parameters
----------------

#### Image

Arbitrary image. Used for reference only.

Type: Image File (`imagej.data.Dataset`)

#### Region of Interest

Set of ROIs which will be measured. Skips non-line-based
ROIs (everything except `imagej.data.overlay.LineOverlay`
and `imagej.data.overlay.PolygonOverlay`).

Type: ROI Set (file) (`imagej.data.overlay.AbstractOverlay[]`)

Results
-------

One result row is produced for each ROI.

#### Length

Length of the ROI (or ROI boundary, for polygons),
in pixels.
