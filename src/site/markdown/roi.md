Regions of Interest (ROIs)
=========================

ImageJ supports image annotation with regions
of interest (ROIs), also called overlays, to
designate image segments or features. The Slide Set core
commands rely on ROIs to identify which parts
of images should be measured (for example,
individual cells). Slide Set includes an editor
to facilitate ROI creation and storage of
ROIs.

Note that while ImageJ allows the creation of
ROIs in arbitrary dimensions (including image channels),
this functionality is not easily
accessible from the user interface
and is ignored by Slide Set. All ROIs created
with the Slide Set ROI editor and used as inputs
for Slide Set core commands are assumed to be
regions in the *X&ndash;Y* plane applying to
all channels, even if they are only drawn on one channel.

The ROI editor
--------------

Open the ROI editor by selecting a table with
images and choosing `Table > ROI Editor` from
the menu. If the table contains more than
one column with images, you will be asked to
select one of them on which the ROIs will be drawn.

The ROI editor panel includes controls for
managing ROI sets and switching between the
images in each row of the data table.

To create a new ROI set, select `Add ROI Set`.
If the table contains multiple ROI sets,
use the drop-down box at the top of the panel
to choose between them. Once you have selected
an ROI set to edit, use the ordinary ROI tools
in the ImageJ toolbar (square, circle, polygon,
line, and point) to draw ROIs on the image.
To save the ROIs in a file (the file name will
be stored in the data table), press `Save`.
Selecting `Undo` will discard any changes
that have been made since the ROIs were last
saved, and reload the ROIs from the file.

To switch between the images in each row of
the data table, select `<<` or `>>`, or
choose an image from the lower drop-down box.
Select `ROI Manager` to open ImageJ's overlay
manager window, which contains more tools for
dealing with ROIs. Select `Export SVG` to
export the ROIs in the displayed image as
an SVG file which can be used in a figure
(not all ROI types can be exported).

Using the ROI editor with *N*-dimensional
images may have unpredictable results, and is not recommended.

ROI file formats
----------------

Slide Set supports two different file formats
for storing ROI data. By default, the ROI editor
saves overlay data as scalable vector graphics (SVG)
files. SVG is a common format for line drawings,
and can be displayed by many web browsers. However,
angle and text overlays cannot be saved in SVG files,
and not all graphical elements that SVG files
can contain can be imported as overlays (for example,
embedded images and ovals that have been rotated or
skewed).

As an alternative, a dedicated file format,
identified by the extension `.roiset`, can be used
to store ROI data. This format is a combination
of individual ROI file formats defined by ImageJ.
While it can handle any overlay type, files
created with one version of ImageJ may not be
readable by later versions.
