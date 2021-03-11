

function getDatePattern(type) {
    var pattern = null;
    if (type == 'DAY') {
        pattern = /(\d{4})-(0[1-9]|1[012])-(31|30|0[1-9]|[12][0-9])/;
    } else if (type == 'MONTH') {
        pattern = /(\d{4})-(0[1-9]|1[012])/;
    } else if (type == 'YEAR') {
        pattern = /(\d{4})/;      
    } else if (type == 'DAYHOURS') {
        pattern = /(\d{4})-(0[1-9]|1[012])-(31|30|0[1-9]|[12][0-9])T(23|22|21|20|[01][0-9]):([012345][0-9]):([012345][0-9])/;
    } else if (type == 'MICROS') {
        pattern = /.(\d{6})/;
    } else {
        pattern = /(\d{4})-(0[1-9]|1[012])-(31|30|0[1-9]|[12][0-9])T(23|22|21|20|[01][0-9]):([012345][0-9]):([012345][0-9]).(\d{6})/;
    } 
    return pattern;
}

function getDate(dateString) {
    // Add 'Z' for UTC date
    var dateNumber = Date.parse(dateString + 'Z');
    if (isNaN(dateNumber)) {
        dateNumber = Date.parse(dateString);
    }
    if (isNaN(dateNumber)) {
        return null;        
    } else {
        return new Date(dateNumber)
    }
}

class ProseoDate {
  constructor(dateString, type) {
    this.type = type;
    this.dateString = dateString;
    var match = dateString.match(getDatePattern(type));
    if (match != null && match.length > 0) {
      var val = match[0];
      if (type == 'DAY') {
        val = val + "T00:00:00.000000";
      } else if (type == 'MONTH') {
        val = val + "-01T00:00:00.000000";
      } else if (type == 'YEAR') {
        val = val + "-01-01T00:00:00.000000";
      }
      var match = val.match(getDatePattern("DAYHOURS"));
      if (match != null) {
       var dateNumber = Date.parse(match[0] + 'Z');
        if (isNaN(dateNumber)) {
            dateNumber = Date.parse(match[0]);
        }
        if (isNaN(dateNumber)) {
          this.date = null;
        } else {
          this.date = new Date(dateNumber);
        }
      } else {
        this.date = null;
      }
    } else {
      this.date = null;
    }
    
    this.micros = 0;
    match = dateString.match(getDatePattern("MICROS"));
    if (match != null && match.length == 2) {
      this.micros = match[1];
    }
  } 
  isValid() {
    return this.date != null;
  } 
  copy() {
    return new ProseoDate(this.dateString, this.type);
  }
  addSeconds(sec) {
    if (this.isValid()) {
      var copyDate = this.copy();
      copyDate.date.setTime(copyDate.date.getTime() + (sec * 1000));
      return copyDate;
    } else {
      return this;
    }
  }
  gt(other) {
    if (this.date.getTime() > other.date.getTime()) {
      return true;
    } else if (   (this.date.getTime() == other.date.getTime())
               && (this.micros > other.micros)) {
      return true;
    }
    return false;
  }
  lt(other) {
    if (this.date.getTime() < other.date.getTime()) {
      return true;
    } else if (   (this.date.getTime() == other.date.getTime())
               && (this.micros < other.micros)) {
      return true;
    }
    return false;
  }
  eq(other) {
    if (   (this.date.getTime() == other.date.getTime())
        && (this.micros == other.micros)) {
      return true;
    }
    return false;
  }
  toString() {
    if (this.isValid()) {
      var dStr = this.date.toISOString().match(getDatePattern("DAYHOURS"))[0];
      dStr += "." + this.micros.toString().padStart(6,0);
      return dStr; 
    }
    return "";
  }
  toTypeString() {
    return this.toString().match(getDatePattern(this.type))[0];
  }
  norm(delta) {
    if (this.isValid()) {
      if (this.type == 'DAY') {
        var y = this.date.setDate(this.date.getDate() - delta);
      } else if (this.type == 'MONTH') {
        var y = this.date.setMonth(this.date.getMonth() - delta);
      } else if (this.type == 'YEAR') {
         var y = this.date.setFullYear(this.date.getFullYear() - delta);
      }
    }
    return this.toTypeString();
  }
}