

$(document).ready( function () {

  //Select all the <h3> elments on the page
  var h3Elements = document.querySelectorAll('h3');

  //Looping through each element to capitalize the first Letter of the table name
  h3Elements.forEach(function(h3Elment){
    h3Elment.textContent = h3Elment.textContent.charAt(0).toUpperCase() + h3Elment.textContent.slice(1);
  });
  $('table').each(function() {
         
    var tableId = $(this).attr('id');  // Get the table id
    var options = {};
    // Check if table ID starts with 'test'
    if (tableId && tableId.startsWith('test')) {
        // Disable paging if table ID starts with 'test'
        options.paging = false;
        // Initialize the DataTable with the appropriate options
        $(this).DataTable(options);
    }       
  });    

});