set terminal svg enhanced font 'Arial,40' size 7680,4320
set output 'divina-commedia-log-log.svg'
set encoding utf8

set title "'Divina Commedia' Word Frequency Log-Log Chart"
set xlabel "Rank (log scale)"
set ylabel "Frequency (log scale))"
set pointsize 1

set logscale xy

stats '../ranks/divina-commedia-ranks.dat' u 2 nooutput
max = STATS_max

plot '../ranks/divina-commedia-ranks.dat' u 1:2 pt 7 with linespoints t 'data',\
     '' u 1:(max/$1) w l t 'ideal Zipf'

save output