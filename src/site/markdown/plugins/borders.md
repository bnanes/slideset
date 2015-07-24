Border Statistics
=================

Calculate signal intensity within sets
of pixels defined by the borders of
regions of interest (ROIs). Returns the mean
value on each channel, the length of
the border, and the number of pixels
included in the border stripe.
This command has two variants, [standard](#std) (`Border Statistics`)
and [multi-channel](#nchan) (`Border Stats (multi-chan)`),
with different structures for input parameters and results.

<h2 id="std">Standard</h2>

### Input Parameters

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

### Results

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



<h2 id="nchan">Multi-channel</h2>

### Input Parameters

#### Image

Expects arbitrary bit-depth images with any number of channels.

Type: Image File (`imagej.data.Dataset`)

#### Region of Interest

Set of ROIs defining the region borders.

Type: ROI Set (file) (`imagej.data.overlay.AbstractOverlay[]`)

#### Border Width

The distance, in pixels, from the ROI borders that will
be included in the analysis.

Type: Numeric (`double`)

#### Thresholds

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

#### Invert

Flag specifying whether pixel values should be measured
above the threshold (`false`, default) or below the
threshold (`true`).

Type: Logical (`boolean`)

### Results

One result row is produced for each image channel within each ROI.

#### Channel

0-based index of the image channel measured.

Type: Integer (`int`)

#### Mean

Mean of threshold-subtracted pixel values within `Border Width`
pixels of the ROI border.

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
