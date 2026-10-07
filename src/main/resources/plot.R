library(lattice)
data <<- numeric(100)

function(dataHolder) {
    svg()
    data <<- c(data[2:100], dataHolder$value)

    plot <- xyplot(randomData ~ time,
        data = data.frame(randomData = data, time = 0:99),
        main = 'Col-16 Plot',
        xlab = 'index', ylab = 'value',
        type = c('l', 'g'),
        col.line = '#5C4033')   # dark brown

    print(plot)
    svg.off()
}