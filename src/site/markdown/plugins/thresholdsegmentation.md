Threshold Segmentation
======================

Segment an image based on defined thresholds on one to three
channels. Returns regions of interest for each 4-neighborhood
segment of pixels above the thresholds.

Input Parameters
----------------

#### Images

Arbitrary bit-depth images with any number of channels.

Type: Image File (`net.imagej.Dataset`)

#### Channel thresholds

A space-separated ordered list of thresholds for each channel. The appropriate
threshold is subtracted from each pixel value. If the pixel
value is less than the threshold, the pixel value is counted as 0.

For example, `10 25 102` would specify a threshold of 10 for the first
channel, a threshold of 25 for the second channel, and a threshold of 102
for the third channel. If an image has fewer channels than
the number of thresholds listed, the extra threshold values will not
be used. If an image has more channels than the number of thresholds
listed, they will be recycled in order. If no thresholds are listed,
a threshold of `0` is implied.

Type: Text (`String`)

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
