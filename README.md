lein-oneoff-code-tests
==========
Clojure code tests that run with plugin `lein-oneoff`

### toilet
-------
Simple clone of the Linux command line utility `wc`.

##### Features
* word count
* line count
* average number of letters per word (to one decimal place)
* most common letter

##### Usage
```
lein oneoff --exec toilet.clj <filename>
# => words:501
#    lines:50
#    average letters per word:3.4
#    most common letter:e
```

### ascii_graphics_editor
-------
Simple ascii "graphics" editor.

##### Features
The editor supports 7 commands:

1. **I** **M** **N**. Create a new M x N image with all pixels
coloured white (O).
2. **C**. Clears the table, setting all pixels to white (O).
3. **L** **X** **Y** **C**. Colours the pixel (X,Y) with colour C.
4. **V** **X** **Y1** **Y2** **C**. Draw a vertical segment of
colour C in column X between rows Y1 and Y2 (inclusive).
5. **H** **X1** **X2** **Y** **C**. Draw a horizontal segment of
colour C in row Y between columns X1 and X2 (inclusive).
6. **F** **X** **Y** **C**. Fill the region R with the colour C.
R is defined as: Pixel (X,Y) belongs to R. Any other pixel which
is the same colour as (X,Y) and shares a common side with any
pixel in R also belongs to this region.
7. **S**. Show the contents of the current image
8. **X**. Terminate the session

##### Usage
Start the editor: `lein oneoff ascii_graphics_editor.clj --exec`

Use the editor. > denotes input, => denotes program output:
```
> I 5 6 
> L 2 3 A 
> S
=> 
OOOOO
OOOOO
OAOOO
OOOOO
OOOOO
OOOOO
> F 3 3 J 
> V 2 3 4 W 
> H 3 4 2 Z 
> S
```

### License
-------
Copyright (C) 2015 Carlos C. Fontes.

Double licensed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) 
(the same as Clojure) or the 
[Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
