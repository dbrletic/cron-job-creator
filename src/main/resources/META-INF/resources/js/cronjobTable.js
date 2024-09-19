
  /* Set the width of the side navigation to 250px */
  function openNav() {
    document.getElementById("mySidenav").style.width = "350px";
  }
  
  /* Set the width of the side navigation to 0 */
  function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
  }
  
  /* Allows you to search the tables */
  function searchTables() {
    const searchInput = document.getElementById('searchInput');
    const searchValue = searchInput.value.trim();
    const rowIndex = 0; // Search in the first column
    const tables = [
      document.getElementById('test1'),
      document.getElementById('test2'),
      document.getElementById('test3'),
      document.getElementById('test4'),
      document.getElementById('test9')
    ];
  
    tables.forEach(table => {
      const rows = table.rows;
      const searchRegex = new RegExp(searchValue, 'i'); // Create a case-insensitive regular expression
  
      // Iterate over each row in the table
      for (let i = 1; i < rows.length; i++) { // Skip the header row
        const row = rows[i];
        const cell = row.cells[rowIndex];
        const cellText = cell.textContent.trim();
  
        // Check if the cell in the specified row contains the search value
        if (searchRegex.test(cellText)) {
          row.style.display = ''; // Show the row
        } else {
          row.style.display = 'none'; // Hide the row
        }
      }
    });
    }
  
    /* Allows for extending of failures */
    document.addEventListener('DOMContentLoaded', function() { 
      var test1 = document.getElementById('test1'); 
      var test2 = document.getElementById('test2'); 
      var test3 = document.getElementById('test3'); 
      var test4 = document.getElementById('test4'); 
      var test9 = document.getElementById('test9'); 

      test1.addEventListener('click', function(e) {
         if (e.target.classList.contains('reveal-cell')){ 
            e.target.classList.toggle('expanded'); 
          } 
      });
      
      test2.addEventListener('click', function(e) {
        if (e.target.classList.contains('reveal-cell')){ 
           e.target.classList.toggle('expanded'); 
         } 
     });

     test3.addEventListener('click', function(e) {
        if (e.target.classList.contains('reveal-cell')){ 
           e.target.classList.toggle('expanded'); 
         } 
     });

     test4.addEventListener('click', function(e) {
        if (e.target.classList.contains('reveal-cell')){ 
           e.target.classList.toggle('expanded'); 
         } 
     });

     test9.addEventListener('click', function(e) {
        if (e.target.classList.contains('reveal-cell')){ 
           e.target.classList.toggle('expanded'); 
         } 
     });
      
     }); 
     
  