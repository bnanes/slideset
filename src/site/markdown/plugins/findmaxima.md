Find Maxima
===========

Find local maxima using the ImageJ `Process > Find Maxima...` command.

Input Parameters
----------------

#### Image

Type: Image File (`imagej.data.Dataset`)

#### Channel (1-index)

Channel on which to identify local maxima, specified as a 1-based index.

Type: Numeric (`int`)

#### Prominence

Height-requirement relative to the local background. Also referred to as
`tolerance` in the ImageJ API documentation. Higher values are more restrictive.
For more information, see the [ImageJ implementation](https://github.com/imagej/ImageJA/blob/master/src/main/java/ij/plugin/filter/MaximumFinder.java).

Type: Numeric (`double`)

#### Strict

If `true`, reject the global maximum if it is below the prominence level.
For more information, see the [ImageJ implementation](https://github.com/imagej/ImageJA/blob/master/src/main/java/ij/plugin/filter/MaximumFinder.java).

Type: Logical (`boolean`)
    
#### Exclude edges

If `true`, exclude local maxima on image edges.

Type: Logical (`boolean`)
    
Results
-------

#### Maxima

Set of point regions of interest (ROIs) identifying the local maxima.

Type: ROI Set (file) (`ii.gui.Roi[]`)
