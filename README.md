Slide Set: Batch Processing for ImageJ
======================================

Slide Set is a framework for batch processing and analysis of image data
using ImageJ.  It simplifies image analysis by linking image files with 
important metadata like regions of interest, experimental conditions, 
and time point information, and automatically repeating analysis tasks 
across multiple regions in multiple images.

* Avoid tedious and error-prone manual repetition without the effort of
  developing a scripted or fully automated solution.
* Ensure the entire image analysis workflow is transparent and reproducible.

Slide Set is a plugin for [ImageJ2](http://developer.imagej.net/). 
It works with [Fiji](http://fiji.sc/Fiji), but not with “plain” 
[ImageJ1](http://imagej.nih.gov/ij/).

More information, including installation instructions and documentation:
<http://cellbio.emory.edu/bnanes/slideset/>

Comments, suggestions, and bug reports are greatly appreciated.

## Features
* Organize data in tables
* Simplify region of interest (ROI) selection
* Save ROIs as [SVG](http://en.wikipedia.org/wiki/Scalable_Vector_Graphics)
  files for reference or reuse in multiple analyses
* Flexible built-in analysis commands, including average pixel values,
  co-localization coefficients, region sizes, and image segmentation
  based on threshold values
* Automate many general ImageJ2 plugins
* Link analysis results to command inputs and track all command parameters
  to maintain transparency and reproducibility
