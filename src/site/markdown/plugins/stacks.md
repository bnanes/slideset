Stack Utilities
===============

Reduce dimensionality of image stacks.

<h2 id="zproj">Z-Project</h2>

Maximum intensity Z projection

### Input Parameters

#### Image

Type: Image File (`ij.ImagePlus`)

### Results

#### Z-Projection

Maximum-intensity projection along the Z axis.

Type: Image File (`imagej.data.Dataset`)

<h2 id="reslice">Reslice</h2>

Re-slice image by vertically sectioning along a line or polyline ROI,
for example, to create XZ planes.

This command is a limited reimplementation of the ImageJ Reslice command
(Image > Stacks > Reslice [/]...) which has been modified to run
without the input dialog box. It ignores calibration data (i.e. uses
1-pixel units rather than distance units).

### Input Parameters

#### Image

Type: Image File (`ij.ImagePlus`)

#### Slice lines

Requires line or polyline type ROIs.

Type: ROI Set (file) (`imagej.data.overlay.AbstractOverlay[]`)
    
#### Flip verticaly

Type: Logical (`boolean`)
    
#### Rotate 90 degrees

Type: Logical (`boolean`)

### Results

#### Slice

Images of the `XZ` planes, where `X` is the distance along the ROI. Returns
one image for each

Type: Image File (`imagej.data.Dataset`)
