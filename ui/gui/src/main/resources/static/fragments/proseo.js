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
    function getURLHash() {
    	var value = location.hash;
    	if (value != null) {
      	  value = unescape(value).replace("+", " ");
      	  value = value.replace("#", "");
      	  return value;
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
      if (param != null && param.length > 0) {
        var loc = location.search.slice(0);
        var paramstring = location.search.slice(1);
        var search = addURLParamPrim(param, paramstring);
        if (search != null) {
          history.pushState({}, null, search);
        }
      }
      return null;
    };
    
    function addURLParamPrim(param, paramstring) {
      var base = paramstring.split("?");
      if (base.length > 1) {
        base = base[1];
      } else {
        base = base[0];
      }
      var pairs = base.split("&");
    	var divider = '?';
    	var first = true;
    	var found = false;
    	var search = '';
    	for (var i = 0; i < pairs.length; i++) {
      	if (pairs[i].length > 0) {
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
    	}
    	if (!found) {
	    	search += divider;
	    	search += param;
	    	return search;
	    }
    	return null;
    };
    
    function addURLParamValuePrim(name, value, paramstring) {
      var searchTmp = null;
      if (value != null && value.length > 0 && name != null && name.length > null) {
        searchTmp = addURLParamPrim(name + '=' + value, paramstring);
        return searchTmp;
      }
      return null;
    };
    
    function addURLParamValuesPrim(name, value, paramstring) {
      var searchTmp = null;
      var search = paramstring;
      if (value != null && value.length > 0 && name != null && name.length > null) {
        for (var i = 0; i < value.length; i++) {
          searchTmp = addURLParamPrim(name + '=' + value[i], search);
          if (searchTmp != null) {search = searchTmp};
        }
        return search;
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
      var search = removeURLKeyPrim(key, paramstring);
      history.pushState({}, null, search);
    };
    
    function removeURLKeyPrim(key, paramstring) {
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
    		} else if (name != null && name.length > 0) {
    			search += divider;
    			search += pairs[i];
	    		if (first) {
	    			divider = '&';
	    			first = false;
	    		}
    		}
    	}
    	return search;
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


    function showLoaderO() {
      document.getElementById("loader").style.display = "block";
    }
    function hideLoaderO() {
        document.getElementById("loader").style.display = "none";
    }

    function showLoader() {
    	$(".loading")[0].style.display = "block";
    }
    function hideLoader() {
        $(".loading")[0].style.display = "none";
    }
    

    function correctScrollPos() {
    	var head = document.getElementById('proseo-thead');
    	var nav = document.getElementById('proseo-nav');
    	var h1 = 0;
    	var h2 = 0;
    	if (head != null) {
    		h1 = head.getBoundingClientRect().height;
    	}
    	if (nav != null) {
    		h2 = nav.getBoundingClientRect().height;
    	}
    	var h = (h1 + h2) * (-1);
    	window.scrollBy(0, h);
    };
    
    function scrollToElem(id) {
    	var ele = document.getElementById(id);
    	if (ele != null) {
    		ele.scrollIntoView();
    		correctScrollPos();
    	}
    };
    
    function scrollToHash() {
        var hash = getURLHash();
        if (hash != null && hash != "") {
            scrollToElem(hash);
            return true;
        }
        return false;
    };
    
    function enableElement(x) {
    	x.disabled = false;
    	if ($(x).hasClass('disabled')) {
    		$(x).removeClass('disabled');
    	}
    	if ($(x).hasClass('font-italic')) {
    		$(x).removeClass('font-italic');
    	}
    	if (!$(x).hasClass('enabled')) {
    		$(x).addClass('enabled');
    	}    	
    };

    function disableElement(x) {
    	x.disabled = true;
    	if (!$(x).hasClass('disabled')) {
    		$(x).addClass('disabled');
    	}
    	if (!$(x).hasClass('font-italic')) {
    		$(x).addClass('font-italic');
    	}
    	if ($(x).hasClass('enabled')) {
    		$(x).removeClass('enabled');
    	}    	
    };
    
    function setSelectedOption(id, value) {
      var val = "";
      if (value != null) {
        val = value;
      }
      var opts = $("#" + id).find("option");
      for (var i = 0; i < opts.length; i++) {
        if ($(opts[i]).val() == val) {
          $(opts[i]).prop( "selected", true );
        } else {
          $(opts[i]).prop( "selected", false );
        }
      }
    }