object Test {
  var (a, b) = (1, 2)
  println(s"a = $a, b = $b")

  // This is the problematic line
  (a, b) = (3, 4)
  println(s"After assignment: a = $a, b = $b")
}
