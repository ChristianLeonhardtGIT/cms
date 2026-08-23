def lines = new File('/tmp/deepen-all-insights.groovy').readLines()
def header = lines[0..6]
def tail = lines[92..(lines.size() - 1)]
def groups = [
    lines[7..40],
    lines[42..68],
    lines[69..89]
]

groups.eachWithIndex { group, index ->
    def source = (header + group + [']'] + tail).join('\n')
    println "Insight-Gruppe ${index + 1} von ${groups.size()} wird verarbeitet."
    new GroovyShell(this.class.classLoader, binding).evaluate(source)
}

println 'Alle Insight-Gruppen wurden verarbeitet.'
