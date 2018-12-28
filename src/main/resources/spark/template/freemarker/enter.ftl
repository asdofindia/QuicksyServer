<#macro enter title>
<!DOCTYPE html>
<html>
<head>
 <meta charset="UTF-8">
 <meta name="viewport" content="width=device-width">
 <title>${title}</title>
<style>
  @font-face{
    font-family:nunito sans;
    font-style:normal;
    font-weight:400;
    src:local("Nunito Sans Regular"),local("NunitoSans-Regular"),url(/fonts/NunitoSans/NunitoSans-Regular.ttf) format("woff2");
    unicode-range:U+0102-0103,U+0110-0111,U+1EA0-1EF9,U+20AB
  }
  @font-face{font-family:nunito sans;
    font-style:normal;
    font-weight:400;
    src:local("Nunito Sans Regular"),local("NunitoSans-Regular"),url(/fonts/NunitoSans/NunitoSans-Regular.ttf) format("woff2");
    unicode-range:U+0100-024F,U+0259,U+1E00-1EFF,U+2020,U+20A0-20AB,U+20AD-20CF,U+2113,U+2C60-2C7F,U+A720-A7FF
  }
  @font-face{
    font-family:nunito sans;
    font-style:normal;
    font-weight:400;
    src:local("Nunito Sans Regular"),local("NunitoSans-Regular"),url(/fonts/NunitoSans/NunitoSans-Regular.ttf) format("woff2");
    unicode-range:U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+2000-206F,U+2074,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD
  }

  @-ms-viewport{
    width: device-width;
  }
  body {
      background-color: #ffffff;
      color: #222222;
      font-family: Nunito Sans,sans-serif;
      font-weight: 400;
      font-size: 13pt;
  }
  h1 {
      font-weight: 400;
      font-size: 24pt;
  }
  .secondary {
      color: #808080;
  }
  .small {
      font-size: 11pt;
  }
  .continue {
      text-align: right;
  }
  .checkout {
      text-align: center;
  }
  a {
      color: #00b8d4;
      text-decoration: none;
  }
  a:hover {
    color: #dbdbdb;
  }
  #box {
      width: 90%;
      max-width: 680px;
      border: 1px solid #dbdbdb;
      margin:0 auto;
      border-radius: 3px;
      background-color: #ffffff;
      padding: 10px;
  }
  input {
      box-sizing: border-box;
      width: 100%;
      font-size: 18pt;
  }

  button {
      font-size: 16pt;
  }

  p.error {
    color: #ff8383;
  }
</style>
</head>
<body>
  <div id="box">
    <h1>${title}</h1>
    <#nested />
  </div>
</body>
</html>
</#macro>
