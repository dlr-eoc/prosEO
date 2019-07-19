
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
  <h1 class="h3 mb-3 font-weight-normal">Please sign in</h1>
        <form name="f" action="/customlogin" method="post" class="form-signin">               
            <fieldset>
			  <select>
			  <option value="s5p">Sentinel S5P</option>
			  <option value="terrax">Terra X </option>
			  <option value="tandem">Tandem </option>
				  </select>
                <legend>Please Login</legend>
                <#if param.error??>
                <div class="alert alert-error">    
                    Invalid username and password.
                </div>
                <#elseif param.logout??>
                <div class="alert alert-success"> 
                    You have been logged out.
                </div>
                </#if>
                <label for="username">Username</label>
                <input type="text" id="username" name="username" class="form-control" />        
                <label for="password">Password</label>
                <input type="password" id="password" name="password" class="form-control" />
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
  <div class="checkbox mb-3">
    <label>
      <input type="checkbox" value="remember-me"> Remember me
    </label>
  </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-lg btn-primary btn-block">Log in</button>
                </div>
            </fieldset>
        </form>  
</body>
</html>
