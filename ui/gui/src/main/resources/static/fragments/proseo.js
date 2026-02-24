/* globals Chart:false, feather:false */

function getLang() {
  var x = document.cookie.lastIndexOf("org.springframework.web.servlet.i18n.CookieLocaleResolver.LOCALE=");
  var lang = "en";
  if (x >= 0) {
    x = "org.springframework.web.servlet.i18n.CookieLocaleResolver.LOCALE=".length;
    lang = document.cookie.substring(x, x + 2);
  }
  return lang;
}

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
    return search;
};

function addURLParamPrim(param, paramstring) {
    var base = trimLeftChar(paramstring, '?');
    var pairs = base.split("&");
    var divider = '?';
    var first = true;
    var found = false;
    var search = "";
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
    return search;
};

function addURLParamValuePrim(name, value, paramstring) {
    var searchTmp = "";
    if (value != null && value.length > 0 && name != null && name.length > null) {
        searchTmp = addURLParamPrim(name + '=' + value, paramstring);
        return searchTmp;
    }
    return paramstring;
};

function addURLParamValuesPrim(name, value, paramstring) {
    var searchTmp = "";
    if (paramstring != null) {
      searchTmp = paramstring;
    }
    if (value != null && value.length > 0 && name != null && name.length > null) {
        for (var i = 0; i < value.length; i++) {
            searchTmp = addURLParamPrim(name + '=' + value[i], searchTmp);
        }
        return searchTmp;
    }
    return paramstring;
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
    var ps = trimLeftChar(paramstring, '?');
    var pairs = ps.split("&");
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

function trimLeftChar(str, char) {
    var s = str;
    while (s.length > 0 && s.charAt(0) == char) {
        s = s.substr(1);
    }
    return s;
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
};

function arrayEquals(a1, a2) {
    if (a1 == null && a2 == null) {
        return true;
    } else if ((a1 == null && a2 != null) || (a1 != null && a2 == null)) {
        return false;
    } else if (a1.length == 0 && a2.length == 0) {
        return true;
    } else if (a1.length != a2.length) {
        return false;
    } else {
        for (var i = 0; i < a1.length; i++) {
            if (!a2.includes(a1[i])) {
                return false;
            }
        }
        return true;
    }
};

function arrayAdd(a, elem) {
    var anArray = a;
    var index = anArray.indexOf(elem);
    if (index < 0) {
        anArray.push(elem);
    }
    return anArray;
}

function arrayRemove(a, elem) {
    var anArray = new Array;
    for(i = 0; i < a.length; i++) {
        if (a[i] != elem) {
            anArray.push(a[i]);
        }
    }
    return anArray;
}


function isEmpty(s) {
    if (s == null || s.trim().length == 0) {
        return true;
    }
    return false;
};

function joinStrings(array, joinString) {
    if (array == null || array.length == 0 || joinString == null) {
        return "";
    }
    var answer = "";
    var firstTime = true;
    for (var i = 0; i < array.length; i++) {
        if (firstTime) {
            firstTime = false;
        } else {
            answer = answer + joinString;
        }
        answer = answer + array[i];
    }
    return answer;
};

function getFromRow() {
	  return (currentPage - 1) * pageSize;
}

function getToRow() {
	  return currentPage * pageSize;
}
  
function setPageParams(newPage, newPageSize) {
	  var hasChanged = false;
    if (newPage == null) {
    	newPage = currentPage;
    } else {
    	newPage = Number(newPage);
    }
    if (isNaN(newPageSize) || newPageSize == null) {
    	newPageSize = pageSize;
    } else {
    	newPageSize = Number(newPageSize);
    }
    if (   newPage != currentPage
    		|| newPageSize != pageSize) {
      hasChanged = true;
    }
    currentPage = Number(newPage);
    pageSize = newPageSize;
    return hasChanged;
}

function setSortParams(ele, selup) {
    var hasChanged = false;  
    var newSortCol = null;
    var newUpText = null;

    if (ele != null && selup != null) {
      newSortCol = ele.replace("select-", "");
      if (selup == "select-up") {
        newUpText = "true";
      } else {
        newUpText = "false";
      }
    } else {
      newSortCol = sortCol;
      newUpText = upText;
    }    
    if (sortCol != newSortCol || upText != newUpText) {
      hasChanged = true;
    }    
    sortCol = newSortCol;
    upText = newUpText;
    return hasChanged;
}

function setPageHTMLParams(paramString) {
    paramString = removeURLKeyPrim('currentPage', paramString);
    paramString = removeURLKeyPrim('pageSize', paramString);
    paramString = addURLParamValuePrim('currentPage', currentPage.toString(), paramString);
    paramString = addURLParamValuePrim('pageSize', pageSize.toString(), paramString);
    return paramString;
}

function setSortHTMLParams(paramString) {
    paramString = addURLParamValuePrim('sortCol', sortCol.replace("select-", ""), paramString);
    paramString = addURLParamValuePrim('up', upText, paramString);
    return paramString;
}

function readPageHTMLParams() {
	  var tmp = getURLParam('currentPage');
	  currentPage = (isEmpty(tmp) || tmp < 1) ? 1 : Number(tmp);
	  tmp = getURLParam('pageSize');
	  pageSize = (isEmpty(tmp) || tmp < 1) ? pageSize : Number(tmp);
}

function readSortHTMLParams() {
    var tmp = getURLParam('sortCol');
    sortCol = isEmpty(tmp) ? (isEmpty(sortCol) ? "id" : sortCol) : tmp;
    tmp = getURLParam('up');
    upText = isEmpty(tmp) ? "true" : tmp;
}

//Function to customize the page size
function updatePageSize() {
	  // Retrieve the new page size
	  var newPageSize = document.getElementById("pageSizeField").value;
	  newPageSize = (!isNaN(newPageSize) && newPageSize > 0) ? parseInt(newPageSize) : pageSize;
	  if (newPageSize != pageSize) { 
	      // pageSize = newPageSize;
	      // Go always to first page
	      currentPage = 1;
	      // Call the function to update the table with the new page size         
	      updateTable(currentPage, newPageSize);
	  }
}

function parameterHasValue(parameterValue) {
  if  (parameterValue != null) {
    if (typeof parameterValue == 'string') {
      return (parameterValue != "null" && parameterValue.trim().length > 0);
    } else {
      return true;
    }
  } else {
    return false;
  }
}

function arrayToHTMLString(anArray) {
  var result = null;
  if (anArray != null && anArray.length > 0) {
    result = "";
    var divider = "";
    for (var i = 0; i < anArray.length; i++) {
      result = result + divider + anArray[i];
      divider = ",";
    }     
  }
  return result;
}

function activateSidebarElem(elemName) {
  var elem = document.getElementById(elemName);
  $(elem).addClass('active');
}

function selectSort(ele) {
    var others = [];
    for (i = 0; i < sortCols.length; i++) {
      if (sortCols[i] != ele) {
        others.push(sortCols[i]);
      }
    }
    var elem = $("#" + ele);
    var sel = "select-down";
    var newsel = "select-up";
    var styles = $(elem).attr("class");
    if (styles != null) {
        var styless = styles.split(" ");
        for (i = 0; i < styless.length; i++) {
            var s = styless[i];
            if (s == "select-up") {
                sel = "select-up";
                newsel = "select-down";
                break;
            } else if (s == "select-down") {
                sel = "select-down";
                newsel = "select-up";
                break;
            }
        }
    }
    $(elem).removeClass(sel);
    $(elem).addClass(newsel);
    for (i = 0; i < others.length; i++) {
        var e = others[i];
        var eleidentifier = $("#" + e);
        $(eleidentifier).removeClass("select-up");
        $(eleidentifier).removeClass("select-down");
    }
    setParams(ele, newsel, null, null);
    retrieveDataPrim(false);
};

function setSort() {
    var ele = sortCol;
    if (ele == null) {
      ele = 'id';
    }
    ele = "select-" + ele;
    var up = true;
    if (upText == "false") {
      up = false;
    }

    let others = new Array(sortCols.length - 1);
    var j = 0;
    for (let i = 0; i < sortCols.length; i++) {
      if (ele != sortCols[i]) {
        others[j] = sortCols[i];
        j++;
      }
    }
    var elem = $("#" + ele);
    var sel = "select-up";
    var newsel = "select-down";
    if (up) {
      sel = "select-down";
      newsel = "select-up";
    }
    $(elem).removeClass(sel);
    $(elem).removeClass(newsel);
    $(elem).addClass(newsel);
    for (i = 0; i < others.length; i++) {
        var e = others[i];
        var eleidentifier = $("#" + e);
        $(eleidentifier).removeClass("select-up");
        $(eleidentifier).removeClass("select-down");
    }
}


function getSort() {
    var done = false;
    var sortCol = sortCols[0].replace("select-", "");
    var up = true;
    
    var i = 0;
    while (!done && i < sortCols.length) {
      var elem = $("#" + sortCols[i]);
      var currSortCol = sortCols[i].replace("select-", "");
      var styles = $(elem).attr("class");
      if (styles != null) {
          var styless = styles.split(" ");
          for (j = 0; j < styless.length; j++) {
              var s = styless[j];
              if (s == "select-up") {
                  done = true;
                  sortCol = currSortCol;
                  up = true;
                  break;
              } else if (s == "select-down") {
                  done = true;
                  sortCol = currSortCol;
                  up = false;
                  break;
              }
          }
      }
      i++;
    }

    return sortCol + ":" + up.toString();
}
