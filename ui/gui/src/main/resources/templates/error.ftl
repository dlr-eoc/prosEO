<!DOCTYPE html>
<html lang ="en">
<head>
<meta charset ="UTF-8">
  <title>Welcome!</title>
</head>
<body>
    <h1>Ooops! Something went wrong</h1>
    <div class = "container">
    <table>
        <tr>
            <td>Date</td>
            <td>${timestamp?datetime}</td>
        </tr>
        <tr>
            <td>Error</td>
            <td>${error}</td>
        </tr>
        <tr>
            <td>Status</td>
            <td>${status}</td>
        </tr>
        <tr>
            <td>Message</td>
            <td>${message}</td>
        </tr>
        <tr>
            <td>Exception</td>
            <td>${exception!"No exception"}</td>
        </tr>
        <tr>
            <td>Trace</td>
            <td>
                <pre>${trace!"No trace"}</pre>
            </td>
        </tr>
    </table>
    </div>
</body>
</html>