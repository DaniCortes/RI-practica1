set terminal svg enhanced font 'Arial,40' size 7680,4320
set output 'war-of-the-worlds.svg'
set encoding utf8

set title "'The War of the Worlds' Word Frequency Line Chart"
set xlabel "Words"
set ylabel "Frequency"
set xtics font "Arial,16"
set xtics rotate by 60 right
set style data linespoints
set key on

plot '../occurrences/war-of-the-worlds-occurrences.dat' using 0:2:xtic(1) with linespoints title ""

save output