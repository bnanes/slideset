Projects and Tables
===================

Slide Set projects organize data into **tables**.
Each table column specifies one field in the
dataset, and each row specifies the individual
values of those fields for one entry in the set.

Any table can be used as input data for a Slide
Set analysis [command or plugin](plugins/index.html). Table
columns are matched to command inputs, and the
analysis is repeated for each table row. Analysis
results are stored in a new table linked to the
input data table.

<table>
  <caption><em>Example Slide Set data table containing images, regions of interest, and group identifiers.</em></caption>
  <tr>
    <th>Image</th><th>ROI</th><th>Group</th>
  </tr>
  <tr>
    <td>img1.tif</td><td>roi/roi1.roiset</td><td>"Control"</td>
  </tr>
  <tr>
    <td>img2.tif</td><td>roi/roi2.roiset</td><td>"Control"</td>
  </tr>
  <tr>
    <td>img3.tif</td><td>roi/roi3.roiset</td><td>"Control"</td>
  </tr>
  <tr>
    <td>img4.tif</td><td>roi/roi4.roiset</td><td>"Treatment A"</td>
  </tr>
  <tr>
    <td>...</td><td>...</td><td>...</td>
  </tr>
</table>

Project files
-------------

Use the `File` menu to create, save, and open
project files. The directory where a project
file is saved will determine the default
storage location for linked resources such
as ROI sets. New projects contain a single
base data table. Results from commands run on
the base table will produce additional tables
within the project.

Project information and all table data
are stored using an XML format. Data from individual
tables can be exported as comma-separated
spreadsheets by selecting `Table > Export Data As CSV`.

Editing tables
--------------

To edit a data table, double-click on the table
in the Slide Set launcher or select `Table > View Table`
from the menu.

Columns and rows can be added to or removed from
the table using the `Column` and `Row` menus.
Each column must have a specified data type,
which defines what information it can store and which
command input variables it can fill. The available
data types are listed below.

Data elements can be entered individually, or
multiple elements (of the same type) can be set together
by selecting them, right-clicking, and choosing
`Set Selected Values` from the popup menu.
Data elements that contain file names, such
as images or ROI sets, can be set by dragging the
image file onto a cell (to change an existing value)
or column header (to add the link in a new row).

To save table data, choose `File > Save` from
the Slide Set launcher menu. To export individual
tables, choose `Table > Export Data As CSV`.

Data types
----------

Four basic data types are available, all of which
are stored directly in the data table:

- Logical (true or false)
- Integer
- Decimal
- Text

Other data types can be stored in separate files,
with the file names listed in the table.
Possible file types include image files
(any format recognized by ImageJ), [regions of interest](roi.html),
and other file formats specified by
[MIME type](http://en.wikipedia.org/wiki/Internet_media_type).
