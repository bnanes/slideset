Pearson's Correlation
=====================

Calculate correlations between two channels
within regions of interest. Returns Pearson's
product-moment correlation coefficient for
pixel values on two channels within each
region of interest (ROI).

Input Parameters
----------------

#### Image

Expects arbitrary bit-depth images with at least 2 channels.

Type: Image File (`imagej.data.Dataset`)

#### Region of Interest

Set of ROIs defining the regions for which correlations will be calculated.

Type: ROI Set (file) (`imagej.data.overlay.AbstractOverlay[]`)

#### Channel 1 (1-based index)

First channel on which to calculate correlations. Specified
using a 1-based index (<code>1 &le; x &le; # of channels</code>).

Type: Integer (`int`)

#### Channel 2 (1-based index)

Second channel on which to calculate correlations. Specified
using a 1-based index (<code>1 &le; x &le; # of channels</code>).

Type: Integer (`int`)

Results
-------

One result row is produced for each ROI.

#### R

Correlation coefficient computed for pixel values on the
selected two channels within the ROI.

<pre>
&sum;[(A<sub>i</sub> &minus; mean(A)) &times; (B<sub>i</sub> &minus; mean(B))] &frasl; sqrt[(A<sub>i</sub> &minus; mean(A))<sup>2</sup> &times; B<sub>i</sub> &minus; mean(B))<sup>2</sup>]
</pre>

<code>A<sub>i</sub></code>, pixel value on first channel    
<code>B<sub>i</sub></code>, pixel value on second channel    
`mean(A)`, mean value on first channel within the ROI    
`mean(B)`, mean value on second channel within the ROI    

Type: Decimal (`float`)
