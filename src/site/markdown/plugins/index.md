Slide Set Commands and Plugins
==============================

Slide Set commands operate on data tables. They take
input values from columns of an existing data table and save
results in a new data table. Each row in the input
table may produce one or more row in the results table,
depending on the command.

Slide Set commands are implemented as ImageJ plugins.
A core set of commands for common analysis tasks are
included. Some general ImageJ commands can also be run with Slide Set,
although there are certain restrictions.
Custom Slide Set commands can be created with only slight changes
from the normal ImageJ plugin development process.

Core Slide Set commands
-----------------------

- [Bin Regions](binregions.html) &mdash;
  Sort a set of regions of interest into bins.

- [Border Statistics](borders.html) &mdash;
  Calculate signal intensity along region
  of interest borders in 3-channel RGB images.

- [Create Mask Image](createmask.html) &mdash;
  Generate a raster representation of regions of interest.

- [Create Points Table](points.html) &mdash;
  Extract coordinates from point regions of interest.

- [Filter Regions](filterregions.html) &mdash;
  Filter a set of regions of interest using
  a second set of regions of interest as a mask.

- [Find Maxima](findmaxima.html) &mdash;
  Identify local maxima in an image.

- [Manders' Coloc. Coefficients](manders.html) &mdash;
  Calculate Manders' colocalization coefficients
  between two channels within regions of interest.

- [Math](math.html) &amp; [Round](round.html) &mdash;
  Basic mathematical functions for numeric data.

- [Otsu Segmentation](otsu.html) &mdash;
  Segment an image based on an automatically
  computed threshold value.

- [Pearson's Correlation](correlation.html) &mdash;
  Calculate correlations between two channels
  within regions of interest.

- [Region Overlap](regionoverlap.html) &mdash;
  Calculate the area of overlap between ROI sets.

- [Region Statistics](regions.html) &mdash;
  Calculate signal intensity within regions of interest.

- [ROI Lengths](length.html) &mdash;
  Calculate lengths of line-based regions of interest.

- [Stack Utilites](stacks.html) &mdash;
  Create maximum intensity projections and re-slice image stacks.

- [Threshold Segmentation](thresholdsegmentation.html) &mdash;
  Segment an image into regions of interest
  based on threshold values.

- [Trainable Weka Segmentation](trainablesegmentation.html) &mdash;
  Segment an image using the Trainable Weka Segmentation plugin.

- [Unmix Absorbances](unmix.html) &mdash;
  Separate an RGB image into two absorbance components.

Running Slide Set commands
--------------------------

To run a command, select the table to use for input values,
then choose the desired command from the
`Table > Run Slide Set Command` menu.

First, you will be asked to select how the command's input parameters
will be filled. Each input parameter is shown next to a
drop-down box containing columns from the input table,
or the option to enter a constant value. Only columns
with an appropriate data type will be listed. If no
column has an appropriate data type, the only option
will be to enter a constant value. Constants are entered
in the text fields to the right of the drop-down boxes.

Next, you will be asked how to handle each of
the commands output values. Most output types,
including numeric and text outputs, can be stored
directly in the results table. Some outputs, such
as images, need to be stored as separate files,
with the names of these files stored in the results table.
For these outputs, you will need to specify a
directory (relative to the location of the project file),
base file name (individual files will be numbered
sequentially), and a file extension (for images,
the file extension determines the image file format
that will be saved). There is also the option to
discard values from any command output.

In addition to selecting how command output
values should be handled, you will have the option
to select columns from the input table to
be copied to the results table. To do so, select
`Include inputs in results`, then choose the
columns that you wish to copy.

Once handling of command inputs and outputs has
been set, the command will be run for each row
in the input table, and the results stored in
a new table. As the command runs, its progress,
as well as any errors, will be tracked in the
log window. The results table will appear in
the table tree once the command completes.

Running other commands with Slide Set (Experimental)
---------------------------------------------------

In addition to the core commands, Slide Set can run general
ImageJ commands, although there are several restrictions
limiting which commands will run successfully.
First, the command must be an ImageJ2 plugin. ImageJ1
plugins, which are distinguished in the menus by a
microscope icon, are not compatible with Slide Set.
Additionally, all of the command's input and output
parameters must use data types recognized by Slide Set.
Image, numeric, and text parameters are generally
compatible. Other data types are less likely to be
recognized.

To run a general ImageJ command, select `Run Other Command`
from the `Table` menu. This will show a list of all
commands and plugins available in ImageJ. Select the command
you wish to run, the press `OK`. Slide Set will then
attempt to run the selected command in a similar manner
to Slide Set commands. The command's progress and any
errors will be shown in the log window.

Creating custom Slide Set commands
----------------------------------

Slide Set is designed to be extensible, and creating new
Slide Set commands requires only a slight variation
on the usual ImageJ2 plugin format. Custom handlers
for data types not recognized by Slide Set are also
possible. More information is available in the API
documentation included with the source code.

<h2 id="cskel">Reusing commands with command skeletons</h2>

Once a sequence of commands has been applied to a table,
they can be exported as a command skeleton which can be
automatically applied to another similarly structured
data table. For example, if `Threshold Segmentation` was
used to identify regions in a set of images and `Region Statistics`
was used to analyze the results, creating a command skeleton
will allow that sequence of commands, with identical settings,
to be applied to another data set. Command skeletons can be
created from any linear sequence of commands, and applied to
any table that has an identical column structure to the original data.

To create a command skeleton, select the final result
table of the sequence of commands you wish to repeat from the tree.
Choose `Table > Command Skeleton > Export...` from the menu bar,
and create a file used to save the command skeleton.

To apply a command skeleton, select the a table from the tree
which will contain the source data for the command series. This table
may be in an entirely different Slide Set project from the project
used to create the command skeleton. However, the source table
must have an identical column structure to the table used as source
data when the command skeleton was created.
Choose `Table > Command Skeleton > Apply...` from the menu bar,
and select the command skeleton file to apply. Each command
will be automatically applied to the source table using the
pre-saved settings.

    Data ┐
         │ (command A)
      Result 1 ┐
               │ (command B)
            Result 2 ┐
                     │ (command C)
                  Result 3

**Example:** Select table `Result 3` to create a command skeleton that will apply
commands A, B, and C in sequence. Selecting table `Result 2` will create
a command skeleton that will apply only commands A and B.
