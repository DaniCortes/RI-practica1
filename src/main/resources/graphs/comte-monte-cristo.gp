set terminal svg enhanced font 'Arial,40' size 7680,4320
set output 'comte-monte-cristo.svg'
set encoding utf8

set title "'Le comte de Montecristo' Word Frequency Line Chart"
set xlabel "Words"
set ylabel "Frequency"
set xtics font "Arial,12"
set xtics rotate by 60 right
set style data linespoints
set key on

plot '../occurrences/comte-monte-cristo-occurrences.dat' using 0:2:xtic(1) with linespoints title ""

save output