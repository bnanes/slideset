Trainable Weka Segmentation
===========================

Use the [Trainable Weka Segmentation](http://fiji.sc/Trainable_Weka_Segmentation)
plugin included with Fiji to classify images. Requires
a classification model file, which must be created
directly with the plugin. Returns image
segmentation or classification probability maps.

*This command is only a wrapper for the Trainable Weka Segmentation
plugin. If the plugin is not installed, the command will not work.*

Input Parameters
----------------

#### Images

Image files to segment.

Type: Image File (`ij.ImagePlus`)

#### Classifier

Classification model file. This file can be created from
the Trainable Weka Segmentation plugin by running the
classifier training process, then selecting `Save Classifier`.
See the [plugin documentation](http://fiji.sc/Trainable_Weka_Segmentation)
for more information.

Type: Weka Model File

#### Save Probabilities

If `true`, save a map of classification probabilities.
If `false` (default), save a segmentation mask.

Type: Logical (`boolean`)

#### RGB Model?

If `true`, treat the image as an 8-bit RGB stack for purposes of the
Weka classifier.
If `false` (default), keep the default image settings.

Type: Logical (`boolean`)

Results
-------

#### Classification

Image containing either a classification probability map
or a segmentation mask, depending on the input value of
`Save Probabilities`.

Type: Image File (`ij.ImagePlus`)

References
----------

- Trainable Weka Segmentation plugin &mdash; <http://fiji.sc/Trainable_Weka_Segmentation>
- Weka machine learning algorithms &mdash; <http://www.cs.waikato.ac.nz/ml/weka/>
