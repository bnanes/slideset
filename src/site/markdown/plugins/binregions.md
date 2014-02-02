Bin Regions
===========

Sort a region of interest (ROI) set into bins.

Input Parameters
----------------

#### Regions

ROIs to sort into bins.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)

#### Bins

ROIs defining the bins used to sort `Regions`.

Type: ROI Set File (`imagej.data.overlay.AbstractOverlay[]`)

Results
-------

One result is produced for each ROI in `Regions`.

#### Bin index

Zero-based index of the bin which contains the region.
A line-based region is contained by a bin if each point
defining the region's perimeter is within the bin.
Other regions are contained by a bin if each integer
coordinate within the region lies within the bin.
If a region is contained by more than one bin, returns
the lowest-indexed bin. If a region is not contained
by any bin, returns `-1`.

Type: Integer (`int`)

#### Row index

Zero-based index of the region's ROI set (input table row).

Type: Integer (`int`)
