Border Statistics
=================

Calculate signal intensity within sets
of pixels defined by the borders of
regions of interest (ROIs) in up to
3-channel RGB images. Returns the mean
value on each channel, the length of
the border, and the number of pixels
included in the border stripe.

Input Parameters
----------------

#### Image

Expects arbitrary bit-depth images with between 1 and 3 channels.
Channels are assumed to be in red, green, blue order.

Type: Image File (`imagej.data.Dataset`)

#### Region of Interest

Set of ROIs defining the region borders.

Type: ROI Set (file) (`imagej.data.overlay.AbstractOverlay[]`)

#### Border Width

The distance, in pixels, from the ROI borders that will
be included in the analysis.

Type: Numeric (`double`)

#### Channel Thresholds

Integers specifying thresholds for each channel. The appropriate
threshold is subtracted from each pixel value. If the pixel
value is less than the threshold, the pixel value is counted as 0.

Type: Numeric (`double`)

#### Invert

Flag specifying whether pixel values should be measured
above the threshold (`false`, default) or below the
threshold (`true`).

Type: Logical (`boolean`)

Results
-------

One result row is produced for each ROI.

#### Red, Green, and Blue Channel Averages

Mean of threshold-subtracted pixel values within the ROI.
For green and blue totals, returns 0 if the image
does not include a second or third channel.

If `Invert` is `false` (default):

<pre>&sum;[max(X<sub>i,c</sub> - T<sub>c</sub>, 0)] / area</pre>

If `Invert` is `true`:

<pre>&ndash;&sum;[min(X<sub>i,c</sub> - T<sub>c</sub>, 0)] / area</pre>
 
<code>X<sub>i,c</sub></code>, value at pixel `i` on channel `c`    
<code>T<sub>c</sub></code>, threshold for channel `c`    
<code>area</code>, number of pixels included in the border region

Type: Numeric (`double`)

#### Length

Length, in pixels, of the border.

Type: Numeric (`double`)

#### Border Stripe Pixels

Number of pixels included within the border region.

Type: Integer (`int`)
