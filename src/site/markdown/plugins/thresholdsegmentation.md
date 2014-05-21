Threshold Segmentation
======================

Segment an image based on defined thresholds on one to three
channels. Returns regions of interest for each 4-neighborhood
segment of pixels above the thresholds.

Input Parameters
----------------

#### Images

Arbitrary bit-depth images with between 1 and 3 channels.

Type: Image File (`net.imagej.Dataset`)

#### Channel thresholds

Integers specifying pixel value thresholds for each channel.
For images with fewer than three channels, additional
threshold values are ignored.

Type: Numeric (`double`)

#### Minimum size

Segmented regions with fewer pixels than this parameter
will be discarded.

Type: Integer (`int`)

#### Maximum size

Segmented regions with more pixels than this parameter
will be discarded.

Type: Integer (`int`)

#### Combine thresholds

If `false`, pixels with values greater than the threshold
on *any* channel may be included in segments. If `true`,
only pixels with values greater than the threshold on
*every* channel may be included in segments.

Type: Logical (`boolean`)

Results
-------

#### Segments

Regions of interest for each 4-neighborhood
segment of pixels above the thresholds.
To view the results using the ROI Editor,
copy the `Images` column from the input
table into the results table.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)
