/* globals Chart:false, feather:false */


    function getURLParam(param) {
    	var paramstring = location.search.slice(1);
    	var pairs = paramstring.split("&");
    	var pair, name, value;
    	for (var i = 0; i < pairs.length; i++) {
    		pair = pairs[i].split("=");
    	  name = pair[0];
    	  value = pair[1];
    	  name = unescape(name).replace("+", " ");
    	  value = unescape(value).replace("+", " ");
    	  if (name == param) {
    	    return value;
    	  }
    	}
    	return null;
    };
    
    function getURLParams(param) {
    	var paramstring = location.search.slice(1);
    	var pairs = paramstring.split("&");
    	var pair, name, value;
    	var params = new Array();
    	for (var i = 0; i < pairs.length; i++) {
    		pair = pairs[i].split("=");
    	  name = pair[0];
    	  value = pair[1];
    	  name = unescape(name).replace("+", " ");
    	  value = unescape(value).replace("+", " ");
    	  if (name == param) {
    	    params.push(value);
    	  }
    	}
    	return params;
    };

    function addURLParam(param) {
    	var loc = location.search.slice(0);
    	var paramstring = location.search.slice(1);
    	var pairs = paramstring.split("&");
    	var divider = '?';
    	var first = true;
    	var found = false;
    	var search = '';
    	for (var i = 0; i < pairs.length; i++) {
    		search += divider;
    		search += pairs[i];
    		if (pairs[i] == param) {
    			found = true;
    		}
    		if (first) {
    			divider = '&';
    			first = false;
    		}
    	}
    	if (!found) {
	    	search += divider;
	    	search += param;
	    	history.pushState({}, null, search);
	    }
    	return null;
    };
    
    function removeURLParam(param) {
    	var loc = location.search.slice(0);
    	var paramstring = location.search.slice(1);
    	var pairs = paramstring.split("&");
    	var divider = '?';
    	var first = true;
    	var search = '';
    	for (var i = 0; i < pairs.length; i++) {
    		if (pairs[i] == param) {
    			// do nothing
    		} else {
    			search += divider;
    			search += pairs[i];
	    		if (first) {
	    			divider = '&';
	    			first = false;
	    		}
    		}
    	}
    	history.pushState({}, null, search);
    	return null;
    };
    function removeURLKey(key) {
    	var loc = location.search.slice(0);
    	var paramstring = location.search.slice(1);
    	var pairs = paramstring.split("&");
    	var name, pair;
    	var divider = '?';
    	var first = true;
    	var search = '';
    	for (var i = 0; i < pairs.length; i++) {
    	    pair = pairs[i].split("=");
      	    name = pair[0];
    		if (name == key) {
    			// do nothing
    		} else {
    			search += divider;
    			search += pairs[i];
	    		if (first) {
	    			divider = '&';
	    			first = false;
	    		}
    		}
    	}
    	history.pushState({}, null, search);
    	return null;
    };
    
    function selectLang(lang) {
    	var loc = location.search.slice(0);
    	var paramstring = location.search.slice(1);
    	var pairs = paramstring.split("&");
    	var pair, name, value;
    	var params = '';
    	var first = true;
    	for (var i = 0; i < pairs.length; i++) {
    		pair = pairs[i].split("=");
    	  name = pair[0];
    	  value = pair[1];
    	  if (!(name == 'lang')) {
    		if (first) {
    			first = false;
    		} else {
    			params += '&';
    		}
    		params = params + name + '=' + value;
    	  }
    	}    	
    	var ref = "?lang=" + lang + "&" + params;
    	location.href = ref;
    	location.reload;
    }


    function showLoader() {
      document.getElementById("loader").style.display = "block";
    }
    function hideLoader() {
        document.getElementById("loader").style.display = "none";
      }