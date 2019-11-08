
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>DLR Mission Login</title>
    <!-- Bootstrap core CSS -->
<!-- Latest compiled and minified CSS -->
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.0/css/bootstrap.min.css">

<!-- jQuery library -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js"></script>

<!-- Latest compiled JavaScript -->
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.0/js/bootstrap.min.js"></script>

    <style>
      .bd-placeholder-img {
        font-size: 1.125rem;
        text-anchor: middle;
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
        user-select: none;
      }

      @media (min-width: 768px) {
        .bd-placeholder-img-lg {
          font-size: 3.5rem;
        }
      }
    </style>
  </head>
  <body class="text-center">
  <div class="container">
  
  <h1 class="h2 mb-2 font-weight-normal">Please sign in</h1>
        <form name="f" action="/customlogin" method="post" class="form-signin"> 
        <div class=form-group">
          <#if RequestParameters.error??>
                <div class="alert alert-error">    
                    Invalid username and password.
                </div>
                
                <#elseif RequestParameters.logout??>
                <div class="alert alert-success"> 
                    You have been logged out.
                </div>
                </#if> 
        <label for="sel1">Select list:</label>
  <select class="form-control" id="sel1">
    <option value="s5p">Sentinel S5P</option>
			  <option value="terrax">Terra X </option>
			  <option value="tandem">Tandem </option>
  </select>
  </div>
  <div class="form-group">
    <label for="username">Username</label>
    <input type="text" class="form-control" id="username" aria-describedby="username" placeholder="Username">
  </div>
  <div class="form-group>     
                <label for="password">Password</label>
                <input type="password" id="password" name="password" class="form-control" />
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    </div>
  <div class="form-actions">
  <button type="submit" class="btn btn-primary">Submit</button>
  </div>
</form>
</body>
</html>

 <!--   <form name="f" action="/customlogin?logout" method="post" class="form-signin"> 
           <input type="submit" value="Logout"/>
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>		
      </form>  -->