# Usage: awk -v pkg=<package> -v foreign=<file> -f logcat-crashes.awk logcat.txt
# Prints PDFBook's crash and ANR lines (these fail the verify run) and writes
# other apps' to the file named by `foreign`. A FATAL EXCEPTION whose
# "Process:" line does not follow within 3 lines counts as PDFBook's.
function own_fatal() { print fatal; fatal = "" }
fatal != "" && /Process: / {
  if (index($0, "Process: " pkg ",") || index($0, "Process: " pkg " ")) { print fatal; print }
  else { print fatal > foreign; print > foreign }
  fatal = ""; next
}
fatal != "" && ++since > 3 { own_fatal() }
/FATAL EXCEPTION/ { if (fatal != "") own_fatal(); fatal = $0; since = 0; next }
/ANR in / { if (index($0, "ANR in " pkg)) print; else print > foreign }
END { if (fatal != "") own_fatal() }
