Add-Type -AssemblyName System.Drawing
foreach ($size in @(192,512)) {
 $bitmap = New-Object System.Drawing.Bitmap($size,$size)
 $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
 $graphics.SmoothingMode = 'AntiAlias'
 $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#F7F3EB'))
 $scale = $size / 512.0
 $graphics.ScaleTransform($scale,$scale)
 $pen = New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml('#29382E'),9)
 foreach ($x in @(126,178,230,282,334,386)) { $graphics.DrawLine($pen,$x,136,$x,376) }
 foreach ($y in @(136,196,256,316,376)) { $graphics.DrawLine($pen,126,$y,386,$y) }
 $brush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#A85432'))
 $graphics.FillEllipse($brush,157,265,42,42)
 $graphics.FillEllipse($brush,209,205,42,42)
 $graphics.FillEllipse($brush,313,145,42,42)
 $pen.Width=7
 $graphics.DrawEllipse($pen,270,90,24,24)
 $graphics.DrawEllipse($pen,374,90,24,24)
 $graphics.DrawLine($pen,116,92,136,112)
 $graphics.DrawLine($pen,136,92,116,112)
 $bitmap.Save((Join-Path (Resolve-Path docs) "icon-$size.png"),[System.Drawing.Imaging.ImageFormat]::Png)
 $graphics.Dispose(); $bitmap.Dispose(); $pen.Dispose(); $brush.Dispose()
}
