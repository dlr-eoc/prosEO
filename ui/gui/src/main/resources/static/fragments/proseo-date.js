
// Get the pattern to match a date string
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

// Class to hold a date with microseconds
class ProseoDate {
  // Create a ProseoDate object by parsing the dateString of type type
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
  
  // Check for valid date
  isValid() {
    return this.date != null;
  } 
  
  // Return a copy of self
  copy() {
    return new ProseoDate(this.dateString, this.type);
  }
  
  // Create a copy and add sec seconds to the date
  addSeconds(sec) {
    if (this.isValid()) {
      var copyDate = this.copy();
      copyDate.date.setTime(copyDate.date.getTime() + (sec * 1000));
      return copyDate;
    } else {
      return this;
    }
  }
  
  // greather than
  gt(other) {
    if (this.date.getTime() > other.date.getTime()) {
      return true;
    } else if (   (this.date.getTime() == other.date.getTime())
               && (this.micros > other.micros)) {
      return true;
    }
    return false;
  }
  
  // less than
  lt(other) {
    if (this.date.getTime() < other.date.getTime()) {
      return true;
    } else if (   (this.date.getTime() == other.date.getTime())
               && (this.micros < other.micros)) {
      return true;
    }
    return false;
  }
  
  // equal
  eq(other) {
    if (   (this.date.getTime() == other.date.getTime())
        && (this.micros == other.micros)) {
      return true;
    }
    return false;
  }
  
  // Return the complete date string "yyyy-mm-ddThh:mm:ss.ssssss"
  toString() {
    if (this.isValid()) {
      var dStr = this.date.toISOString().match(getDatePattern("DAYHOURS"))[0];
      dStr += "." + this.micros.toString().padStart(6,0);
      return dStr; 
    }
    return "";
  }
  
  // Return the date string corresponding to type
  toTypeString() {
    return this.toString().match(getDatePattern(this.type))[0];
  }
  
  // Prepare the date for display in edit box
  norm(delta) {
    if (this.isValid()) {
      if (this.type == 'DAY') {
        this.date.setDate(this.date.getDate() - delta);
      } else if (this.type == 'MONTH') {
        this.date.setMonth(this.date.getMonth() - delta);
      } else if (this.type == 'YEAR') {
         this.date.setFullYear(this.date.getFullYear() - delta);
      }
    }
    return this.toTypeString();
  }
}