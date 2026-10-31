#!/usr/bin/env bash
# usage: measure.sh <label> <row: 0=Rachel 1=Peppa> <col> [cold]
DEV=${DEV:-192.168.0.211:5555}; S=${OUT:-/tmp/smarttube-measure}; mkdir -p "$S"
kp(){ adb -s $DEV shell input keyevent KEYCODE_$1; sleep ${2:-1}; }
LABEL=$1; ROW=$2; COL=$3; COLD=$4
if [ -n "$COLD" ]; then
  adb -s $DEV shell am force-stop org.smarttube.focus
  adb -s $DEV shell am start -n org.smarttube.focus/com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity >/dev/null; sleep 8; kp DPAD_CENTER 2
  kp DPAD_LEFT 1.2; kp DPAD_UP 1; kp DPAD_UP 1; kp DPAD_DOWN 1
else
  kp BACK 1.5; kp BACK 2; kp DPAD_LEFT 1.2
fi
for i in 1 2 3 4; do T=$(adb -s $DEV exec-out uiautomator dump /dev/tty 2>/dev/null | grep -c 'Ms Rachel'); [ "$T" -gt 0 ] && break; kp DPAD_DOWN 2; done
sleep 3; kp DPAD_RIGHT 1.2; [ "$ROW" = 1 ] && kp DPAD_DOWN 1.2; for c in $(seq 1 $COL); do kp DPAD_RIGHT 0.8; done
RATE=$(adb -s $DEV shell dumpsys SurfaceFlinger | grep -o 'refresh-rate.*' | grep -oE '[0-9.]+')
adb -s $DEV logcat -c
T0=$(adb -s $DEV shell 'date +%s%3N; input keyevent 23' | head -1 | tr -d '\r')
sleep 16; adb -s $DEV logcat -d -v epoch > $S/m-$LABEL.log; adb -s $DEV shell input keyevent KEYCODE_MEDIA_PAUSE
python3 - "$S/m-$LABEL.log" "$T0" "$LABEL" "$RATE" <<'PY'
import sys,re
log,t0,label,rate=sys.argv[1],int(sys.argv[2]),sys.argv[3],sys.argv[4]
marks={'player':r'ExoPlayerImpl: Init','potoken':r'TvPoTokenProvider','decoder':r'OMXMaster: makeComponentInstance\(OMX\.MTK\.VIDEO','first':r'Rendering first frame','afr_start':r'UhdHelper: (Sending the broadcast|setPreferredDisplayModeId|onDisplayChanged)','afr_done':r'Callback for expected change','nochange':r'Do not need to change mode'}
out={}
for line in open(log,errors='ignore'):
    m=re.match(r'\s*(\d+\.\d+)',line)
    if not m: continue
    ms=int(float(m.group(1))*1000)-t0
    for k,p in marks.items():
        if k not in out and re.search(p,line): out[k]=ms
print(f"{label:10s} start@{rate}Hz " + "  ".join(f"{k}=+{v/1000:.1f}s" for k,v in sorted(out.items(), key=lambda x:x[1])))
PY
