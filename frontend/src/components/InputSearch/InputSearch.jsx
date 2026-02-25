import SearchIcon from '../icons/Search';
import './InputSearch.css';

export default function InputSearch({ placeholder = "Buscar elemento", onChange, value }) {
    return (
        <div className="inputSearch">
            <input type="text" placeholder={placeholder} onChange={onChange} value={value}/>
            <div className='searchbutton'>
                <SearchIcon width={20} height={20}/>
            </div>
        </div>
    );
}
