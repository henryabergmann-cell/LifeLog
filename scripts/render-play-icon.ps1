# Draws the Play Store icon (store/play-icon-512.png) from the same shapes as the launcher icon
# (app/src/main/res/drawable/ic_launcher_*.xml). Keep the two in sync when the icon changes.
#   powershell -ExecutionPolicy Bypass -File scripts\render-play-icon.ps1 -Out store\play-icon-512.png -Zoom 1.35
# -Zoom enlarges the page: the launcher icon must stay inside Android's safe zone, the store icon need not.
param([string]$Out, [int]$Size = 512, [double]$Zoom = 1.0)
Add-Type -AssemblyName System.Drawing
$s = $Size / 108.0
$bmp = New-Object System.Drawing.Bitmap $Size, $Size
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

function C($hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
function RoundRect($x, $y, $w, $h, $rTop, $rBottom) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $x *= $s; $y *= $s; $w *= $s; $h *= $s; $rt = $rTop * $s * 2; $rb = $rBottom * $s * 2
    if ($rt -gt 0) { $p.AddArc($x, $y, $rt, $rt, 180, 90); $p.AddArc($x + $w - $rt, $y, $rt, $rt, 270, 90) }
    else { $p.AddLine($x, $y, $x + $w, $y) }
    if ($rb -gt 0) { $p.AddArc($x + $w - $rb, $y + $h - $rb, $rb, $rb, 0, 90); $p.AddArc($x, $y + $h - $rb, $rb, $rb, 90, 90) }
    else { $p.AddLine($x + $w, $y + $h, $x, $y + $h) }
    $p.CloseFigure(); $p
}

# Background gradient (top left lighter)
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush ([System.Drawing.PointF]::new(0, 0)), ([System.Drawing.PointF]::new($Size, $Size)), (C '#00897F'), (C '#005353')
$g.FillRectangle($bg, 0, 0, $Size, $Size)
$g.TranslateTransform(54 * $s, 54.5 * $s); $g.ScaleTransform($Zoom, $Zoom); $g.TranslateTransform(-54 * $s, -54.5 * $s)
# Page, header, rings
$g.FillPath((New-Object System.Drawing.SolidBrush (C '#FFFFFF')), (RoundRect 31 35 46 44 8 8))
$g.FillPath((New-Object System.Drawing.SolidBrush (C '#FF8A65')), (RoundRect 31 35 46 12 8 0))
$ring = New-Object System.Drawing.SolidBrush (C '#00393A')
$g.FillPath($ring, (RoundRect 41 30 5 12 2.5 2.5))
$g.FillPath($ring, (RoundRect 62 30 5 12 2.5 2.5))
# Check mark
$pen = New-Object System.Drawing.Pen (C '#006A6A'), (5.5 * $s)
$pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$pts = [System.Drawing.PointF[]]@([System.Drawing.PointF]::new(43 * $s, 62 * $s), [System.Drawing.PointF]::new(50.5 * $s, 69.5 * $s), [System.Drawing.PointF]::new(65.5 * $s, 54.5 * $s))
$g.DrawLines($pen, $pts)

$g.Dispose()
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
