Unmix Absorbances
=================

Separate an RGB image into two absorbance components.
This command is designed for quantification immunohistochemical
stains. For example, diaminobenzidine pigment (brown) can
be isolated from a haematoxylin (blue) counter-stain.
The image is log-transformed to recover absorbance values
on each channel, than a linear unmixing algorithm is used
to apportion absorbance between the two components.
Three images are returned, maps of each absorbance component
and a normalized residual map. The performance of this
command depends on accurate specification of pigment
values for each of the two absorbance components.

Input Parameters
----------------

#### Images

Arbitrary bit-depth RGB image.

Type: Image File (`net.imagej.Dataset`)

#### Pigment colors (R G B)

Color specifications for each of the absorbance components,
formatted as text strings of three numbers, separated by spaces,
in red, green, blue order. These parameters represent
*colors* resulting from an intermediate amount of each pigment.
Best performance is achieved with color specifications
in the middle of the image dynamic range (for example,
`150 60 60` for a red-brown pigment in an 8 bits per channel image).

Type: Text (`String`)

Results
-------

#### Absorbances

Intensity maps of each absorbance component
(<code>a<sub>1</sub></code> and <code>a<sub>2</sub></code>).

For each pixel, minimizes the squared residual of
<code><i><b>y</b></i><sub>rgb</sub> = <b>P</b><i><b>a</b></i><sub>12</sub></code>

<code><i><b>y</b></i><sub>rgb</sub></code>, the RGB absorbance vector,
where absorbance is calculated as <code>-log(<i>value</i>/<i>bit-depth</i>)</code> .    
<code><b>P</b></code>, a matrix specifying pigment absorbances on the red, green, and blue channels     
<code><i><b>a</b></i><sub>12</sub></code>, the absorbance intensity of each component

The returned images are 16-bit.

Type: Image File (`net.imagej.Dataset`)

#### Residual

Normalized residual map, scaled to fit within the image dynamic range.

Type: Image File (`net.imagej.Dataset`)

Reference
----------

Taylor CR and Levenson RM.
Quantification of immunohistochemistry&mdash;issues concerning methods,
utility and semiquantitative assessment II. Histopathology.
2006. doi: 10.1111/j.1365-2559.2006.02513.x
