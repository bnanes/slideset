Manders' Colocalization Coefficients
====================================

Calculate Manders' colocalization coefficients
between two channels within regions of interest.
Returns coefficients M<sub>1</sub> and M<sub>2</sub>
or their signal-weighted variants within each
region of interest (ROI). Manders' colocalization
coefficients are effectively the fraction of signal
on one channel that is colocalized with signal on
another channel. They are strongly dependent on
the selection of appropriate thresholds on each
channel, used to define areas of colocalization.

Input Parameters
----------------

#### Image

Expects arbitrary bit-depth images with at least 2 channels.

Type: Image File (`imagej.data.Dataset`)

#### Region of Interest

Set of ROIs defining the regions for which correlations will be calculated.

Type: ROI Set (file) (`imagej.data.overlay.AbstractOverlay[]`)

#### Channel 1 and 2 (1-based index)

First and second channels on which to calculate correlations. Specified
using 1-based indeces (<code>1 &le; x &le; # of channels</code>).

Type: Integer (`int`)

#### Channel 1 and 2 Thresholds

Threshold values for each channel.

Type: Numeric (`double`)

#### Signal-weighted

If `false`, calculate standard Manders' colocalization coefficients.
If `true`, calculate signal-weighted colocalization coefficients.

Type: Logical (`boolean`)

Results
-------

One result row is produced for each ROI.

#### M1

Manders' colocalization coefficient for signal on the first channel
with signal on the second channel.

Standard:    
<pre>
&sum;<sub>{A+B}</sub>(1) &frasl; &sum;<sub>{A}</sub>(1)
</pre>

Signal-weighted:    
<pre>
&sum;<sub>{A+B}</sub>(A<sub>i</sub>) &frasl; &sum;<sub>{A}</sub>(A<sub>i</sub>)
</pre>

<code>A<sub>i</sub></code>, the value on channel 1 at pixel `i`.    
<code>B<sub>i</sub></code>, the value on channel 2 at pixel `i`.    
`{A}`, the set of pixels where <code>A<sub>i</sub></code> is above the threshold.    
`{A+B}`, the set of pixels where both
<code>A<sub>i</sub></code> and <code>B<sub>i</sub></code>
are above their respective thresholds.

Type: Numeric (`float`)

#### M2

Manders' colocalization coefficient for signal on the second channel
with signal on the first channel.

Standard:    
<pre>
&sum;<sub>{A+B}</sub>(1) &frasl; &sum;<sub>{B}</sub>(1)
</pre>

Signal-weighted:    
<pre>
&sum;<sub>{A+B}</sub>(B<sub>i</sub>) &frasl; &sum;<sub>{B}</sub>(B<sub>i</sub>)
</pre>

<code>A<sub>i</sub></code>, the value on channel 1 at pixel `i`.    
<code>B<sub>i</sub></code>, the value on channel 2 at pixel `i`.    
`{A}`, the set of pixels where <code>A<sub>i</sub></code> is above the threshold.    
`{A+B}`, the set of pixels where both
<code>A<sub>i</sub></code> and <code>B<sub>i</sub></code>
are above their respective thresholds.

Type: Numeric (`float`)

Reference
---------

Dunn KW, Kamocka MM, McDonald JH. A practical guide
to evaluating colocalization in biological microscopy.
Am J Physiol Cell Physiol. 2011.
doi: 10.1152/ajpcell.00462.2010
