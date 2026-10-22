# Usage: awk -v pkg=<package> -v foreign=<file> -f logcat-crashes.awk logcat.txt
# Prints the lines that fail the verify run: every FATAL EXCEPTION (any app,
# as before) and PDFBook's own "ANR in" lines. Other apps' ANRs (for example a
# Pixel Launcher ANR that verify.sh dismissed) go to the file named by `foreign`.
/FATAL EXCEPTION/ { print; next }
/ANR in / { if (index($0, "ANR in " pkg)) print; else print > foreign }
