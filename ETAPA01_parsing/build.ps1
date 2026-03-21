# build.ps1
$antlr = "C:\antlr\antlr-4.13.2-complete.jar"
java -jar $antlr MiniJava.g4
javac -cp ".;$antlr" *.java
Write-Host "Build concluido!" -ForegroundColor Green
