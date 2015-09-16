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

<blockquote><small class="atn">Note that while ImageJ allows the creation of
ROIs in arbitrary dimensions (including image channels),
this functionality is not easily
accessible from the user interface
and is ignored by Slide Set. All ROIs created
with the Slide Set ROI editor and used as inputs
for Slide Set core commands are assumed to be
regions in the <em>X&ndash;Y</em> plane applying to
all channels, even if they are only drawn on one channel.</small></blockquote>

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
Press `t` to add the ROI to the ROI manager,
if it is not added automatically.
To save the ROIs in a file (the file name will
be stored in the data table), select `Save`.
Selecting `Undo` will discard any changes
that have been made since the ROIs were last
saved, and reload the ROIs from the file.

To switch between the images in each row of
the data table, select `<<` or `>>`, or
choose an image from the lower drop-down box.
Select `ROI Manager` to open ImageJ's ROI
manager window, which contains more tools for
dealing with ROIs. Select `Export SVG` to
export the ROIs in the displayed image as
an SVG file which can be used in a figure
(not all ROI types can be exported).

Using the ROI editor with *N*-dimensional
images may have unpredictable results, and is not recommended.

<blockquote id="ij1"><small class="atn">Slide Set contains two versions of the ROI manager,
one using the classic ImageJ user interface and ROI
tools, and one using the ImageJ2 user interface. Prior
to Slide Set version 1.4, only the ImageJ2 version was
included. The current version of Slide Set uses the ImageJ1 user interface by
default. While this results in some minor changes in ROI
selection behavior, this user interface offers improved
stability and performance compared to the ImageJ2 user
interface, which is still considered experimental. The
ImageJ2 ROI Editor is still available by selecting
<code>Table > ROI Editor (IJ2)</code> from the menu. ROIs created
with either version of the ROI editor and saved as SVG files
are fully compatible with both versions.
</small></blockquote>

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
