Otsu Segmentation
=================

Segment an image using Otsu's method for automated
threshold detection. The threshold is selected to
maximize the inter-region variance and minimize
the intra-region variance. Returns optimal threshold
values and binary image maps.

Input Parameters
----------------

#### Images

Arbitrary bit-depth images with any number of channels.

Type: Image File (`net.imagej.Dataset`)

Results
-------

#### Channel

The channel index. Each channel of each source image is processed separately.

Type: Integer (`int`)

#### Threshold

The optimal threshold value which maximizes inter-class variance:

<pre>&#x03C9;<sub>a</sub>(t)&#8201;&#x03C9;<sub>b</sub>(t)&#8201;[&mu;<sub>a</sub>(t)&#8201;-&#8201;&mu;<sub>b</sub>(t)]<sup>2</sup></pre>

Here, <code>t</code> is the threshold value, <code>&#x03C9;<sub>a</sub>(t)</code>
and <code>&#x03C9;<sub>b</sub>(t)</code> are the number of pixels in
each class, and <code>&mu;<sub>a</sub>(t)</code> and <code>&mu;<sub>b</sub>(t)</code>
are the mean pixel values of each class.

Type: Numeric (`double`)

#### Threshold Map

Binary images of each image channel segmented according to
the computed optimal threshold value.

Type: Image File (`net.imagej.Dataset`)

Reference
----------

Nobuyuki Otsu (1979). "A threshold selection method 
from gray-level histograms". IEEE Trans. Sys., Man., Cyber.
9 (1): 62–66. doi:10.1109/TSMC.1979.4310076
