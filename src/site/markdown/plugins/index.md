Slide Set Commands and Plugins
==============================

Slide Set commands operate on data tables. They take
input values from columns of an existing data table and save
results in a new data table. Each row in the input
table may produce one or more row in the results table,
depending on the command.

Slide Set commands are implemented as ImageJ plugins.
A core set of commands for common analysis tasks are
included. Many other plugins can also be run with Slide Set,
although there are certain restrictions.
Custom Slide Set commands can be created with only slight changes
from the normal ImageJ plugin development process.

Core Slide Set commands
-----------------------

- [Region Statistics (RGB)](regions.html) &mdash;
  Calculate signal intensity within regions of
  interest in 3-channel RGB images.

- [Pearson's Correlation](correlation.html) &mdash;
  Calculate correlations between two channels
  within regions of interest.

- [Manders' Coloc. Coefficients](manders.html) &mdash;
  Calculate Manders' colocalization coefficients
  between two channels within regions of interest.

- [ROI Lengths](length.html) &mdash;
  Calculate lengths of line-based regions of interest.

Running other plugins with Slide Set
------------------------------------

Creating custom Slide Set commands
----------------------------------
