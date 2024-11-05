/* Set the width of the side navigation to 250px */
  function openNav() {
    document.getElementById("mySidenav").style.width = "350px";
  }
  
  /* Set the width of the side navigation to 0 */
  function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
  }

    
   $(document).ready( function () {

    //Select all the <h3> elments on the page
    var h3Elements = document.querySelectorAll('h3');

    //Looping through each element to capitalize the first Letter of the table name
    h3Elements.forEach(function(h3Elment){
      h3Elment.textContent = h3Elment.textContent.charAt(0).toUpperCase() + h3Elment.textContent.slice(1);
    });

    //Setting DataTables on all the tables 
    //Have to do it the stupid way since I could not get the ' to escape correctly when creating this in java 
    var test1Element = document.querySelector('#test1');
    var test2Element = document.querySelector('#test2');
    var test3Element = document.querySelector('#test3');
    var test4Element = document.querySelector('#test4');
    var test9Element = document.querySelector('#test9');

    if(test1Element){
      var test1 = new DataTable('#test1', {
          paging: false
        } );
    }
    if(test2Element){
        var test2 = new DataTable('#test2', {
          paging: false
        } );
    }
    if(test3Element){
        var test3 = new DataTable('#test3', {
          paging: false
        } );
    }
    if(test4Element){
        var test4 = new DataTable('#test4', {
          paging: false
        } );
    }
    if(test9Element){
        var test9 = new DataTable('#test9', {
          paging: false
        } );
    }
    }); 