Region Statistics (RGB)
=======================

Calculate signal intensity within regions of
interest in up to 3-channel RGB images. Returns
the sum of individual pixel values on each channel
within each region of interest (ROI), as well as
the area of each ROI.

Input Parameters
----------------

#### Image

Expects arbitrary bit-depth images with between 1 and 3 channels.
Channels are assumed to be in red, green, blue order.

Type: Image File (`imagej.data.Dataset`)

#### Region of Interest

Set of ROIs defining the regions for which statistics will be calculated.

Type: ROI Set (file) (`imagej.data.overlay.AbstractOverlay[]`)

#### Channel Thresholds

Integers specifying thresholds for each channel. The appropriate
threshold is subtracted from each pixel value. If the pixel
value is less than the threshold, the pixel value is counted as 0.

Type: Integer (`int`)

Results
-------

One result row is produced for each ROI.

#### Red, Green, and Blue Channel Totals

Sum of threshold-subtracted pixel values within the ROI.
For green and blue totals, returns 0 if the image
does not include a second or third channel.

<pre>&sum;[max(X<sub>i,c</sub> - T<sub>c</sub>, 0)]</pre>
 
<code>X<sub>i,c</sub></code>, value at pixel `i` on channel `c`    
<code>T<sub>c</sub></code>, threshold for channel `c`

Type: Integer (`int`)

#### Area

Number of pixels within the ROI.

Type: Integer (`int`)
